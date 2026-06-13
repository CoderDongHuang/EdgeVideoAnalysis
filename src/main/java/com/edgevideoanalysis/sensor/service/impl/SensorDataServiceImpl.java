package com.edgevideoanalysis.sensor.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edgevideoanalysis.common.exception.BusinessException;
import com.edgevideoanalysis.alarm.service.IAlarmRecordService;
import com.edgevideoanalysis.sensor.dto.SensorDataDTO;
import com.edgevideoanalysis.sensor.dto.SensorQueryDTO;
import com.edgevideoanalysis.sensor.entity.SensorData;
import com.edgevideoanalysis.sensor.mapper.SensorDataMapper;
import com.edgevideoanalysis.sensor.service.ISensorDataService;
import com.edgevideoanalysis.sensor.service.SensorDataProducer;
import com.edgevideoanalysis.sensor.vo.SensorCurveVO;
import com.edgevideoanalysis.sensor.vo.SensorDataVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorDataServiceImpl implements ISensorDataService {

    private final SensorDataMapper sensorDataMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final IAlarmRecordService alarmRecordService;
    private final SensorDataProducer sensorDataProducer;

    private static final String SENSOR_LATEST_KEY = "sensor:latest:";
    private static final String SENSOR_LOCK_KEY = "sensor:lock:";
    private static final String NULL_MARKER = "__NULL__";

    private static final long CACHE_TTL_SECONDS = 3600;       // 基础TTL 1小时
    private static final long NULL_TTL_SECONDS = 300;         // 空值TTL 5分钟 (防穿透)
    private static final long LOCK_TTL_SECONDS = 10;          // 互斥锁TTL 10秒 (防击穿)
    private static final int  LOCK_RETRY_TIMES = 5;           // 锁重试次数
    private static final long LOCK_RETRY_SLEEP_MS = 50;      // 锁重试间隔

    @Override
    public void reportData(SensorDataDTO dto) {
        // 1. 异步削峰：投递到 RabbitMQ
        boolean sent = sensorDataProducer.sendSensorData(dto);
        if (sent) {
            return; // MQ 投递成功，由 Consumer 异步消费处理
        }

        // 2. MQ 不可用时同步兜底，保证数据不丢失
        log.warn("MQ不可用，降级为同步处理: lampId={}", dto.getLampId());
        processSync(dto);
    }

    /**
     * 同步处理（MQ降级兜底）
     */
    private void processSync(SensorDataDTO dto) {
        SensorData entity = new SensorData();
        entity.setLampId(dto.getLampId());
        entity.setTemperature(dto.getTemperature());
        entity.setHumidity(dto.getHumidity());
        entity.setIllumination(dto.getIllumination());
        entity.setVoltage(dto.getVoltage());
        entity.setCurrent(dto.getCurrent());
        entity.setCaptureTime(dto.getCaptureTime());

        sensorDataMapper.insert(entity);

        // 缓存最新数据到Redis（TTL加随机偏移防雪崩）
        String redisKey = SENSOR_LATEST_KEY + dto.getLampId();
        String cacheData = JSON.toJSONString(dto);
        long ttl = randomizeTtl(CACHE_TTL_SECONDS);
        stringRedisTemplate.opsForValue().set(redisKey, cacheData, ttl, TimeUnit.SECONDS);

        // 检查并触发报警
        alarmRecordService.checkAndCreateAlarm(entity);
    }

    @Override
    public SensorDataVO getLatestData(Long lampId) {
        String redisKey = SENSOR_LATEST_KEY + lampId;

        // 1. 查缓存
        String cacheData = stringRedisTemplate.opsForValue().get(redisKey);
        if (cacheData != null) {
            // 1a. 空值标记（防穿透）：缓存了不存在的数据
            if (NULL_MARKER.equals(cacheData)) {
                throw new BusinessException("未找到传感器数据");
            }
            return JSON.parseObject(cacheData, SensorDataVO.class);
        }

        // 2. 缓存未命中 → 分布式互斥锁防击穿
        String lockKey = SENSOR_LOCK_KEY + lampId;
        boolean lockAcquired = acquireLock(lockKey);
        if (lockAcquired) {
            try {
                // 双重检查：获锁后再次查缓存（其他线程可能已重建）
                cacheData = stringRedisTemplate.opsForValue().get(redisKey);
                if (cacheData != null) {
                    if (NULL_MARKER.equals(cacheData)) {
                        throw new BusinessException("未找到传感器数据");
                    }
                    return JSON.parseObject(cacheData, SensorDataVO.class);
                }
                // 查DB重建缓存
                return rebuildCache(lampId, redisKey);
            } finally {
                releaseLock(lockKey);
            }
        }

        // 3. 未获取锁 → 自旋等待 + 重试读缓存
        for (int i = 0; i < LOCK_RETRY_TIMES; i++) {
            try {
                Thread.sleep(LOCK_RETRY_SLEEP_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            cacheData = stringRedisTemplate.opsForValue().get(redisKey);
            if (cacheData != null) {
                if (NULL_MARKER.equals(cacheData)) {
                    throw new BusinessException("未找到传感器数据");
                }
                return JSON.parseObject(cacheData, SensorDataVO.class);
            }
        }

        // 4. 超时未等到 → 最后兜底直查DB
        log.warn("获取分布式锁超时，直达DB: lampId={}", lampId);
        return rebuildCache(lampId, redisKey);
    }

    /**
     * 查DB并重建缓存
     */
    private SensorDataVO rebuildCache(Long lampId, String redisKey) {
        LambdaQueryWrapper<SensorData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SensorData::getLampId, lampId)
               .orderByDesc(SensorData::getCaptureTime)
               .last("LIMIT 1");
        SensorData data = sensorDataMapper.selectOne(wrapper);

        if (data == null) {
            // 缓存空值，防穿透
            stringRedisTemplate.opsForValue().set(redisKey, NULL_MARKER,
                    randomizeTtl(NULL_TTL_SECONDS), TimeUnit.SECONDS);
            throw new BusinessException("未找到传感器数据");
        }

        SensorDataVO vo = convertToVO(data);
        stringRedisTemplate.opsForValue().set(redisKey, JSON.toJSONString(vo),
                randomizeTtl(CACHE_TTL_SECONDS), TimeUnit.SECONDS);
        return vo;
    }

    /**
     * 获取分布式互斥锁（SETNX）
     */
    private boolean acquireLock(String lockKey) {
        return Boolean.TRUE.equals(
                stringRedisTemplate.opsForValue()
                        .setIfAbsent(lockKey, "1", LOCK_TTL_SECONDS, TimeUnit.SECONDS));
    }

    /**
     * 释放分布式锁
     */
    private void releaseLock(String lockKey) {
        stringRedisTemplate.delete(lockKey);
    }

    /**
     * TTL 加随机偏移（±10%），防雪崩
     */
    private long randomizeTtl(long baseTtlSeconds) {
        long offset = (long) (baseTtlSeconds * 0.1 * ThreadLocalRandom.current().nextDouble());
        return baseTtlSeconds + (ThreadLocalRandom.current().nextBoolean() ? offset : -offset);
    }

    @Override
    public List<SensorDataVO> queryHistory(SensorQueryDTO queryDTO) {
        List<Map<String, Object>> results = sensorDataMapper.queryHistoryData(
                queryDTO.getLampId(),
                queryDTO.getStartTime(),
                queryDTO.getEndTime()
        );

        return results.stream()
                .map(this::mapToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<SensorCurveVO> getCurveData(SensorQueryDTO queryDTO) {
        List<Map<String, Object>> results = sensorDataMapper.queryCurveData(
                queryDTO.getLampId(),
                queryDTO.getSensorType(),
                queryDTO.getStartTime(),
                queryDTO.getEndTime()
        );

        return results.stream()
                .map(this::mapToCurveVO)
                .collect(Collectors.toList());
    }

    private SensorDataVO convertToVO(SensorData entity) {
        SensorDataVO vo = new SensorDataVO();
        vo.setId(entity.getId());
        vo.setLampId(entity.getLampId());
        vo.setTemperature(entity.getTemperature());
        vo.setHumidity(entity.getHumidity());
        vo.setIllumination(entity.getIllumination());
        vo.setVoltage(entity.getVoltage());
        vo.setCurrent(entity.getCurrent());
        vo.setCaptureTime(entity.getCaptureTime());
        return vo;
    }

    private SensorDataVO mapToVO(Map<String, Object> map) {
        SensorDataVO vo = new SensorDataVO();
        vo.setId(((Number) map.get("id")).longValue());
        vo.setLampId(((Number) map.get("lamp_id")).longValue());
        vo.setTemperature(map.get("temperature") != null ? ((Number) map.get("temperature")).doubleValue() : null);
        vo.setHumidity(map.get("humidity") != null ? ((Number) map.get("humidity")).doubleValue() : null);
        vo.setIllumination(map.get("illumination") != null ? ((Number) map.get("illumination")).doubleValue() : null);
        vo.setVoltage(map.get("voltage") != null ? ((Number) map.get("voltage")).doubleValue() : null);
        vo.setCurrent(map.get("current") != null ? ((Number) map.get("current")).doubleValue() : null);
        vo.setCaptureTime((java.time.LocalDateTime) map.get("capture_time"));
        return vo;
    }

    private SensorCurveVO mapToCurveVO(Map<String, Object> map) {
        SensorCurveVO vo = new SensorCurveVO();
        vo.setTime((java.time.LocalDateTime) map.get("time"));
        vo.setValue(map.get("value") != null ? ((Number) map.get("value")).doubleValue() : null);
        return vo;
    }
}

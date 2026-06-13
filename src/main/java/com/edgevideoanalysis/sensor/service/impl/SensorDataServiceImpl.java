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

        // 缓存最新数据到Redis
        String redisKey = SENSOR_LATEST_KEY + dto.getLampId();
        String cacheData = JSON.toJSONString(dto);
        stringRedisTemplate.opsForValue().set(redisKey, cacheData, 1, TimeUnit.HOURS);

        // 检查并触发报警
        alarmRecordService.checkAndCreateAlarm(entity);
    }

    @Override
    public SensorDataVO getLatestData(Long lampId) {
        // 先从Redis缓存获取
        String redisKey = SENSOR_LATEST_KEY + lampId;
        String cacheData = stringRedisTemplate.opsForValue().get(redisKey);
        if (cacheData != null) {
            return JSON.parseObject(cacheData, SensorDataVO.class);
        }

        // 缓存未命中，查询数据库
        LambdaQueryWrapper<SensorData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SensorData::getLampId, lampId)
               .orderByDesc(SensorData::getCaptureTime)
               .last("LIMIT 1");
        SensorData data = sensorDataMapper.selectOne(wrapper);
        if (data == null) {
            throw new BusinessException("未找到传感器数据");
        }

        // 写入缓存
        SensorDataVO vo = convertToVO(data);
        String newCacheData = JSON.toJSONString(vo);
        stringRedisTemplate.opsForValue().set(redisKey, newCacheData, 1, TimeUnit.HOURS);

        return vo;
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

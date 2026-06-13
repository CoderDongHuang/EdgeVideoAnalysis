package com.edgevideoanalysis.sensor.service;

import com.alibaba.fastjson2.JSON;
import com.edgevideoanalysis.alarm.service.IAlarmRecordService;
import com.edgevideoanalysis.common.config.RabbitMQConfig;
import com.edgevideoanalysis.sensor.dto.SensorDataDTO;
import com.edgevideoanalysis.sensor.entity.SensorData;
import com.edgevideoanalysis.sensor.mapper.SensorDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 传感器数据消费者
 * 从 RabbitMQ 消费 → 写库 → 缓存 → 告警判定
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SensorDataConsumer {

    private final SensorDataMapper sensorDataMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final IAlarmRecordService alarmRecordService;

    private static final String SENSOR_LATEST_KEY = "sensor:latest:";

    @RabbitListener(queues = RabbitMQConfig.SENSOR_QUEUE)
    public void handleSensorData(SensorDataDTO dto) {
        log.debug("消费传感器数据: lampId={}", dto.getLampId());

        try {
            // 1. 写入数据库
            SensorData entity = toEntity(dto);
            sensorDataMapper.insert(entity);

            // 2. 缓存最新数据到 Redis（TTL 1小时）
            String redisKey = SENSOR_LATEST_KEY + dto.getLampId();
            stringRedisTemplate.opsForValue().set(
                    redisKey, JSON.toJSONString(dto), 1, TimeUnit.HOURS);

            // 3. 触发告警检查
            alarmRecordService.checkAndCreateAlarm(entity);

            log.debug("传感器数据消费完成: lampId={}", dto.getLampId());
        } catch (Exception e) {
            log.error("消费传感器数据失败: lampId={}", dto.getLampId(), e);
            // 异常会被 RabbitMQ listener retry 处理（最多3次），
            // 全部失败后进入死信队列 sensor.data.dlq
            throw e;
        }
    }

    private SensorData toEntity(SensorDataDTO dto) {
        SensorData entity = new SensorData();
        entity.setLampId(dto.getLampId());
        entity.setTemperature(dto.getTemperature());
        entity.setHumidity(dto.getHumidity());
        entity.setIllumination(dto.getIllumination());
        entity.setVoltage(dto.getVoltage());
        entity.setCurrent(dto.getCurrent());
        entity.setCaptureTime(dto.getCaptureTime());
        return entity;
    }
}

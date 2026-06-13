package com.edgevideoanalysis.sensor.service;

import com.edgevideoanalysis.common.config.RabbitMQConfig;
import com.edgevideoanalysis.sensor.dto.SensorDataDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 传感器数据生产者
 * 将上报数据投递到 RabbitMQ，解耦采集与处理链路
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SensorDataProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 投递传感器数据到消息队列
     * @return true=投递成功  false=投递失败（MQ不可用时同步兜底）
     */
    public boolean sendSensorData(SensorDataDTO dto) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SENSOR_EXCHANGE,
                    RabbitMQConfig.SENSOR_ROUTING_KEY,
                    dto);
            log.debug("传感器数据已投递到MQ: lampId={}", dto.getLampId());
            return true;
        } catch (Exception e) {
            log.error("MQ投递失败，lampId={}", dto.getLampId(), e);
            return false;
        }
    }
}

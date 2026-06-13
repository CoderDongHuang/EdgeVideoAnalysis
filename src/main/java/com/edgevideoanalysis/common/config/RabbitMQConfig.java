package com.edgevideoanalysis.common.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置：传感器数据队列 + 死信队列
 */
@Configuration
public class RabbitMQConfig {

    // ========== 交换机 ==========
    public static final String SENSOR_EXCHANGE = "sensor.data.exchange";
    public static final String SENSOR_DLX_EXCHANGE = "sensor.dlx.exchange";

    // ========== 队列 ==========
    public static final String SENSOR_QUEUE = "sensor.data.queue";
    public static final String SENSOR_DLQ = "sensor.data.dlq";

    // ========== 路由键 ==========
    public static final String SENSOR_ROUTING_KEY = "sensor.data";
    public static final String SENSOR_DLQ_ROUTING_KEY = "sensor.data.dlq";

    @Bean
    public DirectExchange sensorExchange() {
        return new DirectExchange(SENSOR_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange sensorDlxExchange() {
        return new DirectExchange(SENSOR_DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue sensorQueue() {
        return QueueBuilder.durable(SENSOR_QUEUE)
                .deadLetterExchange(SENSOR_DLX_EXCHANGE)
                .deadLetterRoutingKey(SENSOR_DLQ_ROUTING_KEY)
                .ttl(30000) // 消息TTL 30s
                .build();
    }

    @Bean
    public Queue sensorDlq() {
        return QueueBuilder.durable(SENSOR_DLQ).build();
    }

    @Bean
    public Binding sensorBinding() {
        return BindingBuilder.bind(sensorQueue())
                .to(sensorExchange())
                .with(SENSOR_ROUTING_KEY);
    }

    @Bean
    public Binding sensorDlqBinding() {
        return BindingBuilder.bind(sensorDlq())
                .to(sensorDlxExchange())
                .with(SENSOR_DLQ_ROUTING_KEY);
    }
}

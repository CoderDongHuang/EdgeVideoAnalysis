package com.edgevideoanalysis.control.service;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * MQTT 指令下发器
 * 通过 MQTT 协议向边缘设备下发 LED 控制指令
 */
@Slf4j
@Service
public class MqttCommandPublisher {

    private final MqttPahoClientFactory clientFactory;
    private final String brokerUrl;
    private final String clientId;
    private final String commandTopicPrefix;
    private final int qos;

    public MqttCommandPublisher(
            MqttPahoClientFactory clientFactory,
            @Value("${mqtt.broker-url:tcp://localhost:1883}") String brokerUrl,
            @Value("${mqtt.client-id:eva-backend}") String clientId,
            @Value("${mqtt.command-topic-prefix:eva/device}") String commandTopicPrefix,
            @Value("${mqtt.qos:1}") int qos) {
        this.clientFactory = clientFactory;
        this.brokerUrl = brokerUrl;
        this.clientId = clientId + "-" + UUID.randomUUID().toString().substring(0, 8);
        this.commandTopicPrefix = commandTopicPrefix;
        this.qos = qos;
    }

    /**
     * 下发 LED 控制指令到指定设备
     *
     * @param lampId      灯杆ID
     * @param commandType 指令类型 (led_on / led_off)
     * @return 是否下发成功
     */
    public boolean publishCommand(Long lampId, String commandType) {
        String topic = commandTopicPrefix + "/" + lampId + "/command";

        Map<String, Object> payload = new HashMap<>();
        payload.put("commandId", UUID.randomUUID().toString().substring(0, 8));
        payload.put("lampId", lampId);
        payload.put("commandType", commandType);
        payload.put("timestamp", System.currentTimeMillis());

        IMqttClient client = null;
        try {
            client = clientFactory.getClientInstance(brokerUrl, clientId);
            client.connect();

            MqttMessage message = new MqttMessage();
            message.setPayload(JSON.toJSONBytes(payload));
            message.setQos(qos);
            message.setRetained(false);

            client.publish(topic, message);
            log.info("MQTT 指令下发成功: topic={}, commandType={}, lampId={}", topic, commandType, lampId);
            return true;
        } catch (MqttException e) {
            log.error("MQTT 指令下发失败: topic={}, lampId={}, error={}", topic, lampId, e.getMessage());
            return false;
        } finally {
            disconnectQuietly(client);
        }
    }

    private void disconnectQuietly(IMqttClient client) {
        if (client != null && client.isConnected()) {
            try {
                client.disconnect();
            } catch (MqttException e) {
                log.debug("MQTT disconnect 异常: {}", e.getMessage());
            }
        }
    }
}

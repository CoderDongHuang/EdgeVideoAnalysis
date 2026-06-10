package com.edgevideoanalysis.websocket.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 设备WebSocket处理器
 * 处理设备连接、心跳、离线状态
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceWebSocketHandler extends TextWebSocketHandler {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String DEVICE_VERSION_KEY = "device:version:";
    private static final String DEVICE_STATUS_KEY = "device:status:";
    private static final Duration TTL = Duration.ofMinutes(30);

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String lampId = (String) session.getAttributes().get("lampId");
        if (lampId == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        // 初始化Redis版本号
        String versionKey = DEVICE_VERSION_KEY + lampId;
        stringRedisTemplate.opsForValue().set(versionKey, "0", TTL);

        // 更新设备在线状态
        String statusKey = DEVICE_STATUS_KEY + lampId;
        stringRedisTemplate.opsForValue().set(statusKey, "online", TTL);

        log.info("设备连接建立: lampId={}, sessionId={}", lampId, session.getId());

        // 发送连接成功消息
        Map<String, Object> response = new HashMap<>();
        response.put("type", "connected");
        response.put("lampId", lampId);
        session.sendMessage(new TextMessage(JSON.toJSONString(response)));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String lampId = (String) session.getAttributes().get("lampId");
        String payload = message.getPayload();

        try {
            JSONObject json = JSON.parseObject(payload);
            String type = json.getString("type");

            if ("heartbeat".equals(type)) {
                // 处理心跳消息，递增版本号
                String versionKey = DEVICE_VERSION_KEY + lampId;
                String currentVersion = stringRedisTemplate.opsForValue().get(versionKey);
                int newVersion = currentVersion != null ? Integer.parseInt(currentVersion) + 1 : 1;
                stringRedisTemplate.opsForValue().set(versionKey, String.valueOf(newVersion), TTL);

                // 刷新在线状态TTL
                String statusKey = DEVICE_STATUS_KEY + lampId;
                stringRedisTemplate.expire(statusKey, TTL);

                // 返回心跳响应
                Map<String, Object> response = new HashMap<>();
                response.put("type", "heartbeat_ack");
                response.put("version", newVersion);
                response.put("timestamp", System.currentTimeMillis());
                session.sendMessage(new TextMessage(JSON.toJSONString(response)));

                log.debug("心跳处理完成: lampId={}, version={}", lampId, newVersion);
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息失败: lampId={}", lampId, e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String lampId = (String) session.getAttributes().get("lampId");
        if (lampId != null) {
            // 更新设备离线状态
            String statusKey = DEVICE_STATUS_KEY + lampId;
            stringRedisTemplate.opsForValue().set(statusKey, "offline", TTL);

            log.info("设备连接关闭: lampId={}, status={}", lampId, status);
        }
    }
}

package com.edgevideoanalysis.websocket;

import com.edgevideoanalysis.websocket.handler.DeviceWebSocketHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class DeviceWebSocketHandlerTest {

    @Autowired
    private DeviceWebSocketHandler deviceWebSocketHandler;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public void testAfterConnectionEstablished() throws Exception {
        String lampId = "ws_test_1";
        cleanup(lampId);

        // Mock WebSocketSession
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("lampId", lampId);
        when(session.getAttributes()).thenReturn(attributes);
        when(session.getId()).thenReturn("test-session-1");

        // Test connection established
        deviceWebSocketHandler.afterConnectionEstablished(session);

        // Verify Redis
        String version = stringRedisTemplate.opsForValue().get("device:version:" + lampId);
        String status = stringRedisTemplate.opsForValue().get("device:status:" + lampId);

        assertEquals("0", version);
        assertEquals("online", status);

        // Verify connected message sent
        verify(session).sendMessage(any(TextMessage.class));

        cleanup(lampId);
    }

    @Test
    public void testAfterConnectionEstablished_NoLampId() throws Exception {
        // Mock WebSocketSession without lampId
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        when(session.getAttributes()).thenReturn(attributes);

        // Test connection established without lampId
        deviceWebSocketHandler.afterConnectionEstablished(session);

        // Verify session closed
        verify(session).close(CloseStatus.BAD_DATA);
    }

    @Test
    public void testHandleTextMessage_Heartbeat() throws Exception {
        String lampId = "ws_test_2";
        cleanup(lampId);

        // Initialize first
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("lampId", lampId);
        when(session.getAttributes()).thenReturn(attributes);
        when(session.getId()).thenReturn("test-session-2");

        deviceWebSocketHandler.afterConnectionEstablished(session);

        // Send heartbeat message using handleMessage (public method)
        TextMessage heartbeatMessage = new TextMessage("{\"type\":\"heartbeat\"}");
        deviceWebSocketHandler.handleMessage(session, heartbeatMessage);

        // Verify heartbeat ack sent
        verify(session, atLeastOnce()).sendMessage(argThat((WebSocketMessage message) -> {
            try {
                String payload = ((TextMessage) message).getPayload();
                return payload.contains("heartbeat_ack") && payload.contains("version");
            } catch (Exception e) {
                return false;
            }
        }));

        // Verify version incremented
        String version = stringRedisTemplate.opsForValue().get("device:version:" + lampId);
        assertEquals("1", version);

        cleanup(lampId);
    }

    @Test
    public void testAfterConnectionClosed() throws Exception {
        String lampId = "ws_test_3";
        cleanup(lampId);

        // Initialize first
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("lampId", lampId);
        when(session.getAttributes()).thenReturn(attributes);

        deviceWebSocketHandler.afterConnectionEstablished(session);

        // Test connection closed
        deviceWebSocketHandler.afterConnectionClosed(session, CloseStatus.NORMAL);

        // Verify status changed to offline
        String status = stringRedisTemplate.opsForValue().get("device:status:" + lampId);
        assertEquals("offline", status);

        cleanup(lampId);
    }

    private void cleanup(String lampId) {
        stringRedisTemplate.delete("device:version:" + lampId);
        stringRedisTemplate.delete("device:status:" + lampId);
    }
}

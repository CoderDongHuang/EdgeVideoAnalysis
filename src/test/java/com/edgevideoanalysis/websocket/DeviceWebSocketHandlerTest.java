package com.edgevideoanalysis.websocket;

import com.edgevideoanalysis.websocket.handler.DeviceWebSocketHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * DeviceWebSocketHandler 纯 Mockito 单元测试
 * 无需启动 Spring 容器
 */
@ExtendWith(MockitoExtension.class)
class DeviceWebSocketHandlerTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private WebSocketSession session;

    private DeviceWebSocketHandler handler;

    private static final String LAMP_ID = "lamp_test_001";
    private static final String VERSION_KEY = "device:version:lamp_test_001";
    private static final String STATUS_KEY = "device:status:lamp_test_001";

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        handler = new DeviceWebSocketHandler(stringRedisTemplate);
    }

    @AfterEach
    void tearDown() {
        reset(stringRedisTemplate, valueOperations, session);
    }

    // ==================== 连接建立 ====================

    @Test
    @DisplayName("连接建立 → 初始化版本号为0，状态为online，下发 connected")
    void testAfterConnectionEstablished() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("lampId", LAMP_ID);
        when(session.getAttributes()).thenReturn(attrs);
        when(session.getId()).thenReturn("session-1");

        handler.afterConnectionEstablished(session);

        // 验证 Redis 写入了版本号和状态
        verify(valueOperations, times(2)).set(anyString(), anyString(), any(Duration.class));
        // 验证下发 connected 消息
        verify(session).sendMessage(argThat(msg -> {
            String payload = ((TextMessage) msg).getPayload();
            return payload.contains("connected") && payload.contains(LAMP_ID);
        }));
    }

    // ==================== 无 lampId 拒绝 ====================

    @Test
    @DisplayName("连接无 lampId → 关闭会话 BAD_DATA")
    void testAfterConnectionEstablished_NoLampId() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        when(session.getAttributes()).thenReturn(attrs);

        handler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.BAD_DATA);
        // 不应写入 Redis
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    // ==================== 心跳被处理、版本号递增 ====================

    @Test
    @DisplayName("收到心跳 → 版本号递增 → 刷新 TTL → 回复 heartbeat_ack")
    void testHandleHeartbeat() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("lampId", LAMP_ID);
        when(session.getAttributes()).thenReturn(attrs);
        when(valueOperations.get(VERSION_KEY)).thenReturn("5");

        handler.handleMessage(session, new TextMessage("{\"type\":\"heartbeat\"}"));

        // 版本号从 5 变成 6
        verify(valueOperations).set(eq(VERSION_KEY), eq("6"), any(Duration.class));
        // 回复包含版本号 6
        verify(session).sendMessage(argThat(msg ->
                ((TextMessage) msg).getPayload().contains("\"version\":6")));
    }

    // ==================== 首次心跳无版本号 → 从1开始 ====================

    @Test
    @DisplayName("首次心跳 version=null → 版本号初始化为1")
    void testHandleFirstHeartbeat() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("lampId", LAMP_ID);
        when(session.getAttributes()).thenReturn(attrs);
        when(valueOperations.get(VERSION_KEY)).thenReturn(null);

        handler.handleMessage(session, new TextMessage("{\"type\":\"heartbeat\"}"));

        verify(valueOperations).set(eq(VERSION_KEY), eq("1"), any(Duration.class));
    }

    // ==================== 连续三次心跳版本号递增 ====================

    @Test
    @DisplayName("连续三次心跳 → 版本号 5→6→7→8")
    void testConsecutiveHeartbeatsVersionIncrement() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("lampId", LAMP_ID);
        when(session.getAttributes()).thenReturn(attrs);

        // 第一次
        when(valueOperations.get(VERSION_KEY)).thenReturn("5");
        handler.handleMessage(session, new TextMessage("{\"type\":\"heartbeat\"}"));
        verify(valueOperations).set(eq(VERSION_KEY), eq("6"), any(Duration.class));

        // 第二次
        when(valueOperations.get(VERSION_KEY)).thenReturn("6");
        handler.handleMessage(session, new TextMessage("{\"type\":\"heartbeat\"}"));
        verify(valueOperations).set(eq(VERSION_KEY), eq("7"), any(Duration.class));

        // 第三次
        when(valueOperations.get(VERSION_KEY)).thenReturn("7");
        handler.handleMessage(session, new TextMessage("{\"type\":\"heartbeat\"}"));
        verify(valueOperations).set(eq(VERSION_KEY), eq("8"), any(Duration.class));
    }

    // ==================== 连接断开 ====================

    @Test
    @DisplayName("连接断开 → 状态改为 offline")
    void testAfterConnectionClosed() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("lampId", LAMP_ID);
        when(session.getAttributes()).thenReturn(attrs);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(valueOperations).set(anyString(), eq("offline"), any(Duration.class));
    }

    // ==================== 非心跳消息不触发处理 ====================

    @Test
    @DisplayName("非心跳消息 → 不更新版本号、不回复")
    void testHandleNonHeartbeatMessage() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("lampId", LAMP_ID);
        when(session.getAttributes()).thenReturn(attrs);

        handler.handleMessage(session, new TextMessage("{\"type\":\"sensor_report\"}"));

        // 不发送任何消息
        verify(session, never()).sendMessage(any(TextMessage.class));
        // 不读取版本号
        verify(valueOperations, never()).get(anyString());
    }

    // ==================== 畸形JSON不抛异常 ====================

    @Test
    @DisplayName("非法JSON → 不抛异常")
    void testHandleMalformedJson() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("lampId", LAMP_ID);
        when(session.getAttributes()).thenReturn(attrs);

        assertDoesNotThrow(() ->
                handler.handleMessage(session, new TextMessage("not-valid-json{{{")));
    }
}

package com.edgevideoanalysis.websocket.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket握手拦截器
 * 从URL路径中提取灯杆ID并存储到会话属性中
 */
@Slf4j
@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        // 从URL路径中提取灯杆ID: /ws/lamp/{lampId}
        String path = request.getURI().getPath();
        String[] pathParts = path.split("/");
        if (pathParts.length >= 4) {
            String lampId = pathParts[3];
            attributes.put("lampId", lampId);
            log.info("WebSocket握手: lampId={}", lampId);
            return true;
        }
        log.warn("WebSocket握手失败: 无法解析灯杆ID, path={}", path);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("WebSocket握手异常", exception);
        }
    }
}

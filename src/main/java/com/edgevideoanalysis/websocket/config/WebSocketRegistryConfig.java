package com.edgevideoanalysis.websocket.config;

import com.edgevideoanalysis.websocket.handler.DeviceWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket注册配置
 * 注册WebSocket端点并配置握手拦截器
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketRegistryConfig implements WebSocketConfigurer {

    private final DeviceWebSocketHandler deviceWebSocketHandler;

    private final WebSocketHandshakeInterceptor handshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(deviceWebSocketHandler, "/ws/lamp/{lampId}")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins("*");
    }
}

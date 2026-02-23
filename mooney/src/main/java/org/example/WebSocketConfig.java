package org.example;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "websocket.enabled", // 1. application.yml에서 'websocket.enabled' 속성을 찾는다.
    havingValue = "true", // 2. 그 값이 "true"일 때만 이 클래스를 활성화한다.
    matchIfMissing = false // 3. 만약 속성이 없으면 (or 기본값) 비활성화한다.
)
public class WebSocketConfig implements WebSocketConfigurer {

    private final TradeWebSocketHandler tradeWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(tradeWebSocketHandler, "/ws/trade")
                .setAllowedOrigins("*"); // 개발 단계에서는 전체 허용
    }
}

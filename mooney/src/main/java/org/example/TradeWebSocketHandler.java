package org.example;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "websocket.enabled", // 1. application.yml에서 'websocket.enabled' 속성을 찾는다.
    havingValue = "true", // 2. 그 값이 "true"일 때만 이 클래스를 활성화한다.
    matchIfMissing = false // 3. 만약 속성이 없으면 (or 기본값) 비활성화한다.
)
public class TradeWebSocketHandler extends TextWebSocketHandler {

    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    // 체결 이벤트 발생 시 클라이언트로 브로드캐스트
    public void sendTrade(String stockCode, int price) {
        String json = String.format("{\"stock\":\"%s\", \"price\":%d}", stockCode, price);
        for (WebSocketSession s : sessions) {
            try {
                if (s.isOpen()) {
                    s.sendMessage(new TextMessage(json));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

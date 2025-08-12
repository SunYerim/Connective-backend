package com.connective.server.chat.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 메시지 브로커 활성화 (클라이언트가 구독할 prefix)
        config.enableSimpleBroker("/topic", "/queue");
        
        // 클라이언트에서 메시지를 보낼 때 사용할 prefix
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 연결을 위한 엔드포인트 등록
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // CORS 설정 (개발환경용)
                .withSockJS(); // SockJS fallback 지원
    }
}
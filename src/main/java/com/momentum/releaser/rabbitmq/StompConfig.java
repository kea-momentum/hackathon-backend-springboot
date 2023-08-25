package com.momentum.releaser.rabbitmq;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket을 사용하여 STOMP 프로토콜을 구현하기 위한 설정 파일
 */
@Configuration
@EnableWebSocketMessageBroker // WebSocket을 사용하여 메시지 브로커를 활성화한다.
public class StompConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${spring.rabbitmq.host}")
    private String host;

    @Value("${stomp.relay.port}")
    private int relayPort;

    @Value("${spring.rabbitmq.username}")
    private String userName;

    @Value("${spring.rabbitmq.password}")
    private String password;

    /**
     * 클라이언트에서 WebSocket 연결을 맺을 수 있는 엔드 포인트를 등록
     *
     * @param registry StompEndpointRegistry
     * @author seonwoo
     * @date 2023-08-08 (화)
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/notification")
                .setAllowedOriginPatterns("*") // CORS 에러
                .withSockJS();
    }

    /**
     * 메시지 브로커의 구성을 설정
     *
     * @param registry MessageBrokerRegistry
     * @author seonwoo
     * @date 2023-08-08 (화)
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라이언트에서 메시지를 보낼 때 사용할 애플리케이션의 destination prefix
        registry.setApplicationDestinationPrefixes("/notification");

        // STOMP 메시지 전송 시 사용할 경로 구분자 설정
        registry.setPathMatcher(new AntPathMatcher("."));

        // SMTP 브로커 릴레이 활성화
        registry.enableStompBrokerRelay("/queue") // 지정한 경로로 시작하는 모든 메시지는 RabbitMQ에 전달된다.
                .setRelayHost(host)
                .setRelayPort(relayPort)
                .setSystemLogin(userName)
                .setSystemPasscode(password)
                .setClientLogin(userName)
                .setClientPasscode(password);
    }
}

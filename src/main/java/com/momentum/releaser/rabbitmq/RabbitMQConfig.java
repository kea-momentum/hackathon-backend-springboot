package com.momentum.releaser.rabbitmq;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * WebSocket & RabbitMQ Configuration
 */
@Configuration
@EnableRabbit
public class RabbitMQConfig {

    @Value("${spring.rabbitmq.host}")
    private String host;

    @Value("${spring.rabbitmq.port}")
    private int port;

    @Value("${spring.rabbitmq.username}")
    private String userName;

    @Value("${spring.rabbitmq.password}")
    private String password;

    /**
     * RabbitMQ 연결을 위한 ConnectionFactory Bean을 생성하여 반환
     *
     * @return ConnnectionFactory 객체
     * @author seonwoo
     * @date 2023-08-04 (금)
     */
    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        connectionFactory.setUsername(userName);
        connectionFactory.setPassword(password);
        return connectionFactory;
    }

    /**
     * RabbitTemplate을 생성하여 반환
     *
     * @param connectionFactory RabbitMQ와의 연결을 위한 ConnectionFactory 객체
     * @return RabbitTemplate 객체
     * @author seonwoo
     * @date 2023-08-04 (금)
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);

        // JSON 형식의 메시지를 직렬화하고 역직렬을 할 수 있도록 설정
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter());
        return rabbitTemplate;
    }

    /**
     * Jackson 라이브러리를 사용하여 메시지를 JSON 형식으로 변환하는 MessageConverter Bean을 생성
     *
     * @return MessageConverter 객체
     * @author seonwoo
     * @date 2023-08-04 (금)
     */
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

//    @Bean
//    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
//        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
//        builder.modules(new JavaTimeModule()); // Java 8 날짜/시간 모듈 추가
//        return builder;
//    }

    /**
     * 동적으로 큐를 생성한다.
     *
     * @param connectionFactory ConnectionFactory
     * @return RabbitAdmin
     * @author seonwoo
     * @date 2023-08-07 (월)
     */
    @Bean
    public AmqpAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    /**
     * 사용자 가입할 때 큐를 생성하고 바인딩하는 메서드
     *
     * @return DirectExchange
     * @author seonwoo
     * @date 2023-08-07 (월)
     */
    @Bean
    public DirectExchange userDirectExchange() {
        return new DirectExchange("releaser.user");
    }

    /**
     * LocalDateTime 데이터를 JSON 형식으로 직렬화 및 역직렬하도록 도와준다.
     *
     * @return com.fasterxml.jackson.databind.Module
     * @author seonwoo
     * @date 2023-08-08 (화)
     */
//    @Bean
//    public Module dateTimeModule() {
//        return new JavaTimeModule();
//    }
}

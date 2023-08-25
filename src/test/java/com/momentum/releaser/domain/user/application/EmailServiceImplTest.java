package com.momentum.releaser.domain.user.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

import static com.momentum.releaser.global.config.BaseResponseStatus.INVALID_REDIS_KEY;

import com.momentum.releaser.global.exception.CustomException;
import com.momentum.releaser.redis.password.Password;
import com.momentum.releaser.redis.password.PasswordRedisRepository;

@EnableConfigurationProperties
@SpringBootTest
class EmailServiceImplTest {

    @Autowired
    private PasswordRedisRepository passwordRedisRepository;

    /**
     * Redis에 비밀번호 변경 인증을 위한 데이터 저장
     */
//    @DisplayName("Redis에 비밀번호 변경 인증을 위한 데이터 저장 성공")
//    @Test
//    void sendEmailForPassword() {
//        // given
//        Password password = Password.builder()
//                .email("ska2870ghk@naver.com")
//                .name("남선우")
//                .expiredTime(60) // 테스트용 1분
//                .build();
//
//        // when
//        Password savedPassword = passwordRedisRepository.save(password);
//
//        // then
//        Password findPassword = passwordRedisRepository.findById(password.getEmail()).orElseThrow(() -> new CustomException(INVALID_REDIS_KEY));
//        System.out.println(savedPassword.getEmail());
//        System.out.println(findPassword.getEmail());
//        assertEquals(savedPassword.getEmail(), findPassword.getEmail());
//    }
}

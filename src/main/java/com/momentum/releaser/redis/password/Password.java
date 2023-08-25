package com.momentum.releaser.redis.password;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RedisHash(value = "password")
public class Password {

    // 사용자 이메일을 기준으로 Redis 키 설정
    @Id
    private String email;

    private String name;

    private String code;

    @TimeToLive
    private long expiredTime;

    @Builder
    public Password(String name, String email, String code, long expiredTime) {
        this.name = name;
        this.email = email;
        this.code = code;
        this.expiredTime = expiredTime;
    }
}

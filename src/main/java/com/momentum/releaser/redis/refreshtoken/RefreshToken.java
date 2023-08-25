package com.momentum.releaser.redis.refreshtoken;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.index.Indexed;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RedisHash(value = "refresh-token")
public class RefreshToken {
    @Id
    private String refreshToken;

    @Indexed
    private String userEmail;

    @TimeToLive
    private long expiredTime;

    @Builder
    public RefreshToken(String refreshToken, String userEmail, long expiredTime) {
        this.refreshToken = refreshToken;
        this.userEmail = userEmail;
        this.expiredTime = expiredTime;
    }

    /**
     * 새로운 Refresh Token으로 업데이트한다.
     *
     * @author seonwoo
     * @date 2023-08-15 (화)
     * @param refreshToken Refresh Token 값
     * @return 현재 객체
     */
    public RefreshToken updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
        return this;
    }
}

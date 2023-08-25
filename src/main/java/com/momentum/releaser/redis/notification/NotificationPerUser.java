package com.momentum.releaser.redis.notification;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RedisHash(value = "notification-per-user")
public class NotificationPerUser {
    @Id
    private String email;
    private List<String> notifications;
    @TimeToLive
    private long expiredTime;

    @Builder
    public NotificationPerUser(String email, List<String> notifications, long expiredTime) {
        this.email = email;
        this.notifications = notifications;
        this.expiredTime = expiredTime;
    }

    public void updateNotifications(List<String> notifications) {
        this.notifications = notifications;
    }
}

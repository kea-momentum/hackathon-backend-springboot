package com.momentum.releaser.redis.notification;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.annotation.Id;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RedisHash(value = "notification")
public class Notification {

    @Id
    private String notificationId;

    private String type;
    private Long projectId;

    private String projectTitle;

    private String projectImg;

    private String message;

    private String date;

    private Map<String, Integer> markByUsers = new HashMap<>();

    @TimeToLive
    private long expiredTime;

    @Builder
    public Notification(String notificationId, String type, Long projectId, String projectTitle, String projectImg, String message, String date, HashMap<String, Integer> markByUsers, long expiredTime) {
        this.notificationId = notificationId;
        this.type = type;
        this.projectId = projectId;
        this.projectTitle = projectTitle;
        this.projectImg = projectImg;
        this.message = message;
        this.date = date;
        this.markByUsers = markByUsers;
        this.expiredTime = expiredTime;
    }

    /**
     * 사용자 읽음 여부 데이터를 업데이트한다.
     *
     * @author seonwoo
     * @date 2023-08-15 (화)
     * @param key 사용자 이메일
     * @param value 읽음 여부 데이터
     */
    public void updateMarkByUsers(String key, int value) {
        this.markByUsers.put(key, value);
    }
}

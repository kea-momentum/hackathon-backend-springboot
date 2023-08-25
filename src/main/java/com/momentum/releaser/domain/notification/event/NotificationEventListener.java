package com.momentum.releaser.domain.notification.event;

import java.text.SimpleDateFormat;
import java.util.*;

import com.momentum.releaser.global.exception.CustomException;
import com.momentum.releaser.redis.notification.Notification;
import com.momentum.releaser.redis.notification.NotificationPerUser;
import com.momentum.releaser.redis.notification.NotificationPerUserRedisRepository;
import com.momentum.releaser.redis.notification.NotificationRedisRepository;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties.Jedis;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static com.momentum.releaser.global.config.BaseResponseStatus.NOT_EXISTS_NOTIFICATION_PER_USER;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {
    // RabbitMQ
    private final RabbitTemplate rabbitTemplate;
    private final DirectExchange userDirectExchange;
    private final DirectExchange projectDirectExchange;

    // Redis
    private final NotificationRedisRepository notificationRedisRepository;
    private final NotificationPerUserRedisRepository notificationPerUserRedisRepository;

    /**
     * 릴리즈 노트 알림 이벤트
     *
     * @param releaseNoteMessageEvent 릴리즈 노트 알림 이벤트
     * @author seonwoo
     * @date 2023-08-14 (월)
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReleaseNoteEvent(final ReleaseNoteMessageEvent releaseNoteMessageEvent) {
        List<String> consumers = releaseNoteMessageEvent.getConsumers();

        if (releaseNoteMessageEvent.getType() == ConsumerType.USER) {
            // 알림 타입이 사용자인 경우 해당 사용자 개별 큐로 메시지를 전송한다.
            for (String consumer : consumers) {
                String routingKey = "releaser.user." + consumer;
                rabbitTemplate.convertAndSend(userDirectExchange.getName(), routingKey, releaseNoteMessageEvent.getMessage());
            }
        }

        if (releaseNoteMessageEvent.getType() == ConsumerType.PROJECT) {
            // 알림 타입이 프로젝트인 경우 해당 프로젝트 큐로 메시지를 전송한다.
            String routingKey = "releaser.project." + releaseNoteMessageEvent.getMessage().getProjectId();
            rabbitTemplate.convertAndSend(projectDirectExchange.getName(), routingKey, releaseNoteMessageEvent.getMessage());
        }

        // Redis에 데이터를 저장한다.
        saveReleaseNoteNotificationToRedis(releaseNoteMessageEvent);
    }

    /**
     * 이슈 알림 이벤트
     *
     * @param issueMessageEvent 이슈 알림 이벤트
     * @author seonwoo
     * @date 2023-08-14 (월)
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIssueEvent(final IssueMessageEvent issueMessageEvent) {
        List<String> consumers = issueMessageEvent.getConsumers();

        if (issueMessageEvent.getType() == ConsumerType.USER) {
            // 알림 타입이 사용자인 경우 해당 사용자 개별 큐로 메시지를 전송한다.
            for (String consumer : consumers) {
                String routingKey = "releaser.user." + consumer;
                rabbitTemplate.convertAndSend(userDirectExchange.getName(), routingKey, issueMessageEvent.getMessage());
            }
        }

        if (issueMessageEvent.getType() == ConsumerType.PROJECT) {
            // 알림 타입이 프로젝트인 경우 해당 프로젝트 큐로 메시지를 전송한다.
            String routingKey = "releaser.project." + issueMessageEvent.getMessage().getProjectId();
            rabbitTemplate.convertAndSend(projectDirectExchange.getName(), routingKey, issueMessageEvent.getMessage());
        }

        // Redis에 데이터를 저장한다.
        saveIssueNotificationToRedis(issueMessageEvent);
    }

    /**
     * 릴리즈 노트 알림 메시지와 필요한 정보들을 Redis에 저장한다.
     *
     * @param notificationEvent 알림 메시지 이벤트
     * @author seonwoo
     * @date 2023-08-11 (금)
     */
    private void saveReleaseNoteNotificationToRedis(ReleaseNoteMessageEvent notificationEvent) {
        // 사용자들의 알림 확인 여부를 체크하기 위해 데이터를 추가한다.
        HashMap<String, Integer> markByUsers = new HashMap<>();
        List<String> consumers = notificationEvent.getConsumers();
        for (String consumer : consumers) {
            markByUsers.put(consumer, 0);
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateAsString = dateFormat.format(notificationEvent.getMessage().getDate());

        // Redis에 저장하기 위한 데이터를 생성한다.
        Notification notification = Notification.builder()
                .notificationId(notificationEvent.getEventId())
                .type("Release Note")
                .projectId(notificationEvent.getMessage().getProjectId())
                .projectTitle(notificationEvent.getMessage().getProjectName())
                .projectImg(notificationEvent.getMessage().getProjectImg())
                .message(notificationEvent.getMessage().getMessage())
                .date(dateAsString)
                .markByUsers(markByUsers)
                .expiredTime(604800) // 일주일
                .build();

        notificationRedisRepository.save(notification);
        saveNotificationPerUserToRedis(notification, consumers);
    }

    /**
     * 이슈 알림 메시지와 필요한 정보들을 Redis에 저장한다.
     *
     * @param notificationEvent 알림 이벤트
     * @author seonwoo
     * @date 2023-08-14 (월)
     */
    private void saveIssueNotificationToRedis(IssueMessageEvent notificationEvent) {
        // 사용자들의 알림 확인 여부를 체크하기 위해 데이터를 추가한다.
        HashMap<String, Integer> markByUsers = new HashMap<>();
        List<String> consumers = notificationEvent.getConsumers();
        for (String consumer : consumers) {
            markByUsers.put(consumer, 0);
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateAsString = dateFormat.format(notificationEvent.getMessage().getDate());

        Notification notification = Notification.builder()
                .notificationId(notificationEvent.getEventId())
                .type("Issue")
                .projectId(notificationEvent.getMessage().getProjectId())
                .projectTitle(notificationEvent.getMessage().getProjectName())
                .projectImg(notificationEvent.getMessage().getProjectImg())
                .message(notificationEvent.getMessage().getMessage())
                .date(dateAsString)
                .markByUsers(markByUsers)
                .expiredTime(604800) // 일주일
                .build();

        notificationRedisRepository.save(notification);
        saveNotificationPerUserToRedis(notification, consumers);
    }

    /**
     * 사용자별 알림 데이터를 Redis에 저장한다.
     *
     * @param notification 알림 데이터
     * @param consumers    알림 소비자(대상) 목록
     * @author seonwoo
     * @date 2023-08-14 (월)
     */
    private void saveNotificationPerUserToRedis(Notification notification, List<String> consumers) {
        for (String consumer : consumers) {
            Optional<NotificationPerUser> optionalNotificationPerUser = notificationPerUserRedisRepository.findById(consumer);

            if (optionalNotificationPerUser.isEmpty()) {
                NotificationPerUser notificationPerUser = NotificationPerUser.builder()
                        .email(consumer)
                        .expiredTime(604800)
                        .build();

                notificationPerUserRedisRepository.save(notificationPerUser);
            }

            NotificationPerUser notificationPerUser = notificationPerUserRedisRepository.findById(consumer)
                    .orElseThrow(() -> new CustomException(NOT_EXISTS_NOTIFICATION_PER_USER));

            // 사용자별 알림 데이터에 현재 발생한 알림 데이터를 저장한다.
            List<String> notifications = notificationPerUser.getNotifications();
            if (notifications == null) {
                notifications = new ArrayList<>();
            }
            notifications.add(notification.getNotificationId());

            notificationPerUser.updateNotifications(notifications);
            notificationPerUserRedisRepository.save(notificationPerUser);
        }
    }
}

package com.momentum.releaser.redis.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import java.util.Map;
import java.util.Optional;

public interface NotificationRedisRepository extends CrudRepository<Notification, String> {

    Optional<Notification> findByNotificationId(String notificationId);

    Page<Notification> findByMarkByUsersOrderByDateDesc(Map<String, Integer> markByUsers, Pageable pageable);

    Optional<Notification> findByNotificationIdAndMarkByUsers(String notificationId, Map<String, Integer> markByUsers);
}

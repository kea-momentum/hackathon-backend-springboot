package com.momentum.releaser.redis.notification;

import org.springframework.data.repository.CrudRepository;

public interface NotificationPerUserRedisRepository extends CrudRepository<NotificationPerUser, String> {
}

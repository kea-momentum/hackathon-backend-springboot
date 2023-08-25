package com.momentum.releaser.redis.issue;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface OrderIssueRedisRepository extends CrudRepository<OrderIssue, Long> {
    Optional<OrderIssue> findByProjectId(Long projectId);
}

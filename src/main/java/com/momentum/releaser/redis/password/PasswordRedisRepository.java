package com.momentum.releaser.redis.password;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface PasswordRedisRepository extends CrudRepository<Password, String> {
    Optional<Password> findByNameAndEmail(String name, String email);

    boolean existsPasswordByNameAndEmail(String name, String email);
}

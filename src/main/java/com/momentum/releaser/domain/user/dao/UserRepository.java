package com.momentum.releaser.domain.user.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.momentum.releaser.domain.user.domain.User;

@RepositoryRestResource(collectionResourceRel="user", path="user")
public interface UserRepository extends JpaRepository<User, Long> {

    // 이메일을 사용하여 사용자 정보 조회
    Optional<User> findOneByEmail(String email);
    // 이메일을 사용하여 사용자 정보 조회
    Optional<User> findByEmail(String email);

    User getUserByEmail(String s);
}

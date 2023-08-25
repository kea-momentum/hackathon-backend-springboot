package com.momentum.releaser.domain.user.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.momentum.releaser.domain.user.domain.AuthPassword;
import com.momentum.releaser.domain.user.domain.User;


@RepositoryRestResource(collectionResourceRel="auth-password", path="auth-password")
public interface AuthPasswordRepository extends JpaRepository<AuthPassword, Long> {

    // AuthPassword 엔티티에서 User 객체 해당하는 AuthPassword 찾기
    AuthPassword findByUser(User user);
}

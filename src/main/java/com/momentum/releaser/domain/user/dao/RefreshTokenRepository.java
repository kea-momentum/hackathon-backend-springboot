package com.momentum.releaser.domain.user.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.momentum.releaser.domain.user.domain.RefreshToken;

@RepositoryRestResource(collectionResourceRel="refresh-token", path="refresh-token")
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // User 이메일(email)을 사용하여 해당하는 RefreshToken 찾기
    Optional<RefreshToken> findByUserEmail(String email);
}
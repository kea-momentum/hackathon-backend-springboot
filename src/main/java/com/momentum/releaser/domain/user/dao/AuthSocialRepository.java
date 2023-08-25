package com.momentum.releaser.domain.user.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.momentum.releaser.domain.user.domain.AuthSocial;
import com.momentum.releaser.domain.user.domain.User;


import java.util.Optional;

@RepositoryRestResource(collectionResourceRel="auth-social", path="auth-social")
public interface AuthSocialRepository extends JpaRepository<AuthSocial, Long> {

    // Optional<User>를 인자로 받아 해당하는 AuthSocial 찾기
    Optional<AuthSocial> findByUser(Optional<User> userOptional);
}

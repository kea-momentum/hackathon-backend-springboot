package com.momentum.releaser.redis.refreshtoken;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

public interface RefreshTokenRedisRepository extends CrudRepository<RefreshToken, String> {

    /**
     * 사용자 이메일(userEmail)을 사용하여 해당하는 Refresh Token 값을 찾는다.
     *
     * @author seonwoo
     * @date 2023-08-15 (화)
     * @param userEmail 사용자 이메일
     * @return Optional로 감싸진 RefreshToken
     */
    Optional<RefreshToken> findByUserEmail(String userEmail);
}

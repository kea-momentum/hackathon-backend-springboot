package com.momentum.releaser.domain.user.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "refresh_token")
@Entity
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank
    private String refreshToken;
    @NotBlank
    private String userEmail;

    public RefreshToken(String token, String email) {
        this.refreshToken = token;
        this.userEmail = email;
    }

    /**
     * 새로운 Refresh Token으로 업데이트
     */
    public RefreshToken updateToken(String token) {
        this.refreshToken = token;
        return this;
    }
}

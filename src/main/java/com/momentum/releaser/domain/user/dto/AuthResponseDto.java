package com.momentum.releaser.domain.user.dto;

import lombok.*;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;

public class AuthResponseDto {

    /**
     * 2.1 회원가입
     *
     * 회원가입 성공 시 사용자 정보를 담은 DTO
     */
    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class UserInfoResponseDTO {
        private Long userId;
        private String name;
        private String email;

        @Builder
        public UserInfoResponseDTO(Long userId, String name, String email) {
            this.userId = userId;
            this.name = name;
            this.email = email;
        }
    }

    /**
     * 2.7 이메일 인증 확인
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ConfirmEmailResponseDTO {
        private String email;

        @Builder
        public ConfirmEmailResponseDTO(String email) {
            this.email = email;
        }
    }

    /**
     * 2.9 비밀번호 변경 인증 확인
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ConfirmPasswordCodeResponseDTO {
        private String email;
        private String name;

        @Builder
        public ConfirmPasswordCodeResponseDTO(String email, String name) {
            this.email = email;
            this.name = name;
        }
    }
}

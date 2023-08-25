package com.momentum.releaser.domain.user.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserRequestDto {

    /**
     * 1.2 사용자 프로필 이미지 변경
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class UserUpdateImgRequestDTO {
        private String image;

        @Builder
        public UserUpdateImgRequestDTO(String image) {
            this.image = image;
        }
    }
}

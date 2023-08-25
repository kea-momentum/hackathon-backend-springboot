package com.momentum.releaser.domain.notification.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class NotificationRequestDto {

    /**
     * 11.2 릴리즈 노트 배포 동의 알림
     *
     * @author seonwoo
     * @date 2023-08-14 (월)
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class NotificationApprovalRequestDto {
        Long releaseId;

        @Builder
        public NotificationApprovalRequestDto(Long releaseId) {
            this.releaseId = releaseId;
        }
    }
}

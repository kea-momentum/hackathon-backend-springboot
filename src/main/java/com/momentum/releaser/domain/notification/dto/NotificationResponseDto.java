package com.momentum.releaser.domain.notification.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class NotificationResponseDto {

    /**
     * 11.1 사용자별 알림 내역 조회
     *
     * @author seonwoo
     * @date 2023-08-14 (월)
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class NotificationListResponseDto {
        private String notificationId;
        private String type;
        private Long projectId;
        private String projectTitle;
        private String projectImg;
        private String message;
        private String date;
        private int isRead;

        @Builder
        public NotificationListResponseDto(String notificationId, String type, Long projectId, String projectTitle, String projectImg, String message, String date, int isRead) {
            this.notificationId = notificationId;
            this.type = type;
            this.projectId = projectId;
            this.projectTitle = projectTitle;
            this.projectImg = projectImg;
            this.message = message;
            this.date = date;
            this.isRead = isRead;
        }

        /**
         * 사용자 알림 확인 여부 확인
         *
         * @param isRead 알림 확인 여부
         * @author seonwoo
         * @date 2023-08-14 (월)
         */
        public void updateIsRead(int isRead) {
            this.isRead = isRead;
        }
    }
}

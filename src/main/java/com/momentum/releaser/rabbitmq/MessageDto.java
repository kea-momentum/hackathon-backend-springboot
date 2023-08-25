package com.momentum.releaser.rabbitmq;

import java.util.Date;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class MessageDto {

    /**
     * Releaser 프로젝트 알림
     *
     * @author seonwoo
     * @date 2023-08-14 (월)
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ReleaserMessageDto {
        private String projectName;
        private String message;
        private String type;
        private Date date;

        @Builder
        public ReleaserMessageDto(String projectName, String message, String type, Date date) {
            this.projectName = projectName;
            this.message = message;
            this.type = type;
            this.date = date;
        }
    }

    /**
     * 릴리즈 노트 유형 알림 메시지
     *
     * @author seonwoo
     * @date 2023-08-14 (월)
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ReleaseNoteMessageDto {
        private String type;
        private Long projectId;
        private String projectName;
        private String projectImg;
        private String message;
        private Date date;
        private Long releaseNoteId;

        @Builder
        public ReleaseNoteMessageDto(String type, Long projectId, String projectName, String projectImg, String message, Date date, Long releaseNoteId) {
            this.type = type;
            this.projectId = projectId;
            this.projectName = projectName;
            this.projectImg = projectImg;
            this.message = message;
            this.date = date;
            this.releaseNoteId = releaseNoteId;
        }
    }

    /**
     * 이슈 유형 알림 메시지
     *
     * @author seonwoo
     * @date 2023-08-14 (월)
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class IssueMessageDto {
        private String type;
        private Long projectId;
        private String projectName;
        private String projectImg;
        private String message;
        private Date date;
        private Long issueId;

        @Builder
        public IssueMessageDto(String type, Long projectId, String projectName, String projectImg, String message, Date date, Long issueId) {
            this.type = type;
            this.projectId = projectId;
            this.projectName = projectName;
            this.projectImg = projectImg;
            this.message = message;
            this.date = date;
            this.issueId = issueId;
        }
    }
}

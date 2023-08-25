package com.momentum.releaser.domain.project.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.querydsl.core.annotations.QueryProjection;

import java.util.Date;

public class ProjectDataDto {

    /**
     * 프로젝트 멤버 조회
     */
    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class GetMembersDataDTO {
        private Long memberId;
        private Long userId;
        private String name;
        private String img;
        private char position;

        @QueryProjection
        @Builder
        public GetMembersDataDTO(Long memberId, Long userId, String name, String img, char position) {
            this.memberId = memberId;
            this.userId = userId;
            this.name = name;
            this.img = img;
            this.position = position;
        }
    }

    /**
     * 개별 프로젝트 조회
     */
    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class GetProjectDataDTO {
        private Long projectId;
        private String title;
        private String content;
        private String team;
        private String img;

        @Builder
        public GetProjectDataDTO(Long projectId, String title, String content, String team, String img) {
            this.projectId = projectId;
            this.title = title;
            this.content = content;
            this.team = team;
            this.img = img;
        }
    }

    /**
     * 릴리즈 노트 정보 조회
     */
    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class GetReleaseInfoDataDTO {

        private Long releaseId;
        private String version;
        private String title;
        private Date deployDate;
        private Long pmId;
        private String pmName;
        private String pmImg;

        @Builder
        public GetReleaseInfoDataDTO(Long releaseId, String version, String title, Date deployDate, Long pmId, String pmName, String pmImg) {
            this.releaseId = releaseId;
            this.version = version;
            this.title = title;
            this.deployDate = deployDate;
            this.pmId = pmId;
            this.pmName = pmName;
            this.pmImg = pmImg;
        }
    }

    /**
     * 10.1 프로젝트 내 통합검색 - 이슈 정보
     */
    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class GetIssueInfoDataDTO {

        private Long issueId;
        private String title;
        private String tag;
        private String releaseVersion;
        private Date endDate;
        private Long manager;
        private String managerName;
        private String managerImg;

        @Builder
        public GetIssueInfoDataDTO(Long issueId, String title, String tag, String releaseVersion, Date endDate, Long manager, String managerName, String managerImg) {
            this.issueId = issueId;
            this.title = title;
            this.tag = tag;
            this.releaseVersion = releaseVersion;
            this.endDate = endDate;
            this.manager = manager;
            this.managerName = managerName;
            this.managerImg = managerImg;
        }
    }

}

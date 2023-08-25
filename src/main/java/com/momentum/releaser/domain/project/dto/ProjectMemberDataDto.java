package com.momentum.releaser.domain.project.dto;

import lombok.*;

import com.querydsl.core.annotations.QueryProjection;

public class ProjectMemberDataDto {

    /**
     * 릴리즈 노트 모달 하단에 프로젝트 멤버들의 프로필을 보여주는 부분
     * 5.2 릴리즈 노트 생성
     * 5.4 릴리즈 노트 조회
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ProjectMembersDataDTO {
        private Long memberId;
        private String name;
        private String profileImg;

        @Builder
        public ProjectMembersDataDTO(Long memberId, String name, String profileImg) {
            this.memberId = memberId;
            this.name = name;
            this.profileImg = profileImg;
        }
    }

    /**
     * 프로젝트 멤버 정보
     */
    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ProjectMemberInfoDTO {
        private Long memberId;
        private Long userId;
        private String name;
        private String img;
        private char position;
        private char deleteYN;

        @QueryProjection
        @Builder
        public ProjectMemberInfoDTO(Long memberId, Long userId, String name, String img, char position) {
            this.memberId = memberId;
            this.userId = userId;
            this.name = name;
            this.img = img;
            this.position = position;
        }
    }

}

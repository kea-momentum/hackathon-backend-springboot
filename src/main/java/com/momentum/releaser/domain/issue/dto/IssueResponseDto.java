package com.momentum.releaser.domain.issue.dto;

import java.util.Date;
import java.util.List;

import com.momentum.releaser.domain.issue.dto.IssueDataDto.IssueDetailsDataDTO;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.querydsl.core.annotations.QueryProjection;

import com.momentum.releaser.domain.project.dto.ProjectDataDto.GetMembersDataDTO;

public class IssueResponseDto {

    /**
     * 이슈 상태 구분하여 이슈 조회
     */
    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class AllIssueListResponseDTO {
        private List<IssueInfoResponseDTO> getNotStartedList;
        private List<IssueInfoResponseDTO> getInProgressList;
        private List<IssueInfoResponseDTO> getDoneList;

        @Builder
        public AllIssueListResponseDTO(List<IssueInfoResponseDTO> getNotStartedList, List<IssueInfoResponseDTO> getInProgressList, List<IssueInfoResponseDTO> getDoneList) {
            this.getNotStartedList = getNotStartedList;
            this.getInProgressList = getInProgressList;
            this.getDoneList = getDoneList;
        }
    }

    /**
     * 이슈 수정한 멤버 보내주기
     */
    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class IssueModifyResponseDTO {
        private Long memberId;
        private char position;

        @Builder
        public IssueModifyResponseDTO(Long memberId, char position) {
            this.memberId = memberId;
            this.position = position;
        }
    }

    /**
     * 이슈 정보
     */
    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class IssueInfoResponseDTO {

        private Long issueId;
        private Long issueNum;
        private String title;
        private String content;
        private Date endDate;
        private Long memberId;
        private String memberName;
        private String memberImg;
        private String tag;
        private String releaseVersion;
        private char edit;
        private String lifeCycle;
        private char deployYN;

        @Builder
        @QueryProjection
        public IssueInfoResponseDTO(Long issueId, Long issueNum, String title, String content, Date endDate, Long memberId, String memberName, String memberImg, String tag, String releaseVersion, char edit, String lifeCycle) {
            this.issueId = issueId;
            this.issueNum = issueNum;
            this.title = title;
            this.content = content;
            this.endDate = endDate;
            this.memberId = memberId;
            this.memberName = memberName;
            this.memberImg = memberImg;
            this.tag = tag;
            this.releaseVersion = releaseVersion;
            this.edit = edit;
            this.lifeCycle = lifeCycle;
        }
    }

    /**
     * 프로젝트별 연결 가능한 이슈
     */
    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class DoneIssuesResponseDTO {
        private Long issueId;
        private Long issueNum;
        private String title;
        private String tag;
        private Date endDate;
        private char edit;
        private Long memberId;
        private String memberName;
        private String memberImg;

        @Builder
        @QueryProjection
        public DoneIssuesResponseDTO(Long issueId, Long issueNum, String title, String tag, Date endDate, char edit, Long memberId, String memberName, String memberImg) {
            this.issueId = issueId;
            this.issueNum = issueNum;
            this.title = title;
            this.tag = tag;
            this.endDate = endDate;
            this.edit = edit;
            this.memberId = memberId;
            this.memberName = memberName;
            this.memberImg = memberImg;
        }
    }

    /**
     * 프로젝트별 릴리즈와 연결된 이슈
     */
    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ConnectionIssuesResponseDTO {
        private Long issueId;
        private Long issueNum;
        private String title;
        private String tag;
        private char edit;
        private Long memberId;
        private String memberName;
        private String memberImg;
        private String releaseVersion;

        @Builder
        @QueryProjection
        public ConnectionIssuesResponseDTO(Long issueId, Long issueNum, String title, String tag, char edit, Long memberId, String memberName, String memberImg, String releaseVersion) {
            this.issueId = issueId;
            this.issueNum = issueNum;
            this.title = title;
            this.tag = tag;
            this.edit = edit;
            this.memberId = memberId;
            this.memberName = memberName;
            this.memberImg = memberImg;
            this.releaseVersion = releaseVersion;
        }
    }

    /**
     * 이슈별 조회
     */
    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class IssueDetailsDTO {
        private char pmCheck;
        private IssueDetailsDataDTO issueDetails;

        @Builder
        public IssueDetailsDTO(char pmCheck, IssueDetailsDataDTO issueDetails) {
            this.pmCheck = pmCheck;
            this.issueDetails = issueDetails;
        }
    }

    /**
     * 이슈 의견 정보
     */
    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class OpinionInfoResponseDTO {
        private Long memberId;
        private String memberName;
        private String memberImg;
        private Long opinionId;
        private String opinion;
        private char deleteYN;

        @Builder
        @QueryProjection
        public OpinionInfoResponseDTO(Long memberId, String memberName, String memberImg, Long opinionId, String opinion) {
            this.memberId = memberId;
            this.memberName = memberName;
            this.memberImg = memberImg;
            this.opinionId = opinionId;
            this.opinion = opinion;
        }
    }

    /**
     * 생성 및 수정한 이슈 식별 번호 정보
     */
    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class IssueIdResponseDTO {
        private Long issueId;
        private Long issueNum;

        @Builder
        public IssueIdResponseDTO(Long issueId, Long issueNum) {
            this.issueId = issueId;
            this.issueNum = issueNum;
        }
    }

}

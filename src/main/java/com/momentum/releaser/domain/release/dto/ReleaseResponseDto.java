package com.momentum.releaser.domain.release.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.*;

import com.momentum.releaser.domain.issue.dto.IssueDataDto.ConnectedIssuesDataDTO;
import com.momentum.releaser.domain.project.dto.ProjectMemberResponseDto.ProjectMemberPositionResponseDTO;
import com.momentum.releaser.domain.release.domain.ReleaseEnum.ReleaseDeployStatus;
import com.momentum.releaser.domain.release.dto.ReleaseDataDto.GetTagsDataDTO;
import com.momentum.releaser.domain.release.dto.ReleaseDataDto.ReleaseApprovalsDataDTO;
import com.momentum.releaser.domain.release.dto.ReleaseDataDto.ReleaseOpinionsDataDTO;
import com.momentum.releaser.domain.release.dto.ReleaseDataDto.ReleasesDataDTO;

public class ReleaseResponseDto {

    /**
     * 5.1 프로젝트별 릴리즈 노트 목록 조회
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ReleasesResponseDTO {
        // 프로젝트 정보
        private Long projectId;
        private String title;
        private String team;
        private String img;

        private ProjectMemberPositionResponseDTO member;

        // 릴리즈 노트 목록
        private List<ReleasesDataDTO> releases;

        @Builder
        public ReleasesResponseDTO(Long projectId, String title, String team, String img, ProjectMemberPositionResponseDTO member, List<ReleasesDataDTO> releases) {
            this.projectId = projectId;
            this.title = title;
            this.team = team;
            this.img = img;
            this.member = member;
            this.releases = releases;
        }
    }

    /**
     * 5.2 릴리즈 노트 생성
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ReleaseCreateAndUpdateResponseDTO {
        private Long releaseId;
        private String version;
        private String summary;
        private Date deployDate;
        private ReleaseDeployStatus deployStatus;
        private Double coordX;
        private Double coordY;

        @Builder

        public ReleaseCreateAndUpdateResponseDTO(Long releaseId, String version, String summary, Date deployDate, ReleaseDeployStatus deployStatus, Double coordX, Double coordY) {
            this.releaseId = releaseId;
            this.version = version;
            this.summary = summary;
            this.deployDate = deployDate;
            this.deployStatus = deployStatus;
            this.coordX = coordX;
            this.coordY = coordY;
        }
    }

    /**
     * 5.5 릴리즈 노트 조회
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ReleaseInfoResponseDTO {
        private Long releaseId;
        private String title;
        private String content;
        private String summary;
        private String version;
        private Date deployDate;
        private String deployStatus;
        private List<ConnectedIssuesDataDTO> issues;
        private List<ReleaseOpinionsDataDTO> opinions;
        private List<ReleaseApprovalsDataDTO> approvals;

        @Builder
        public ReleaseInfoResponseDTO(Long releaseId, String title, String content, String summary, String version, Date deployDate, String deployStatus, List<ConnectedIssuesDataDTO> issues, List<ReleaseOpinionsDataDTO> opinions, List<ReleaseApprovalsDataDTO> approvals) {
            this.releaseId = releaseId;
            this.title = title;
            this.content = content;
            this.summary = summary;
            this.version = version;
            this.deployDate = deployDate;
            this.deployStatus = deployStatus;
            this.issues = issues;
            this.opinions = opinions;
            this.approvals = approvals;
        }
    }

    /**
     * 5.6 릴리즈 노트 배포 동의 여부 선택 (멤버용)
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ReleaseApprovalsResponseDTO {
        private Long memberId;
        private String memberName;
        private String memberImg;
        private char position;
        private char approval;

        @Builder
        public ReleaseApprovalsResponseDTO(Long memberId, String memberName, String memberImg, char position, char approval) {
            this.memberId = memberId;
            this.memberName = memberName;
            this.memberImg = memberImg;
            this.position = position;
            this.approval = approval;
        }
    }

    /**
     * 6.1 릴리즈 노트 의견 추가 (이전)
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ReleaseOpinionCreateResponseDTO {
        private Long releaseOpinionId;

        @Builder
        public ReleaseOpinionCreateResponseDTO(Long releaseOpinionId) {
            this.releaseOpinionId = releaseOpinionId;
        }
    }

    /**
     * 6.1 릴리즈 노트 의견 추가
     * 6.2 릴리즈 노트 의견 삭제
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ReleaseOpinionsResponseDTO {
        private Long opinionId;
        private String opinion;
        private Long memberId;
        private String memberName;
        private String memberImg;
        private char deleteYN;

        @Builder
        public ReleaseOpinionsResponseDTO(Long opinionId, String opinion, Long memberId, String memberName, String memberImg, char deleteYN) {
            this.opinionId = opinionId;
            this.opinion = opinion;
            this.memberId = memberId;
            this.memberName = memberName;
            this.memberImg = memberImg;
            this.deleteYN = deleteYN;
        }

        public void updateDeleteYN(char deleteYN) {
            this.deleteYN = deleteYN;
        }
    }

    /**
     * 9.1 프로젝트별 릴리즈 보고서 조회
     */
    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ReleaseDocsResponseDTO {
        private Long releaseId;
        private String releaseVersion;
        private String releaseTitle;
        private String releaseContent;
        private List<GetTagsDataDTO> tagsList = new ArrayList<>(); // 빈 리스트로 초기화

        @Builder
        public ReleaseDocsResponseDTO(Long releaseId, String releaseVersion, String releaseTitle, String releaseContent, List<GetTagsDataDTO> tagsList) {
            this.releaseId = releaseId;
            this.releaseVersion = releaseVersion;
            this.releaseTitle = releaseTitle;
            this.releaseContent = releaseContent;
            if (tagsList != null) {
                this.tagsList = tagsList;
            }
        }
    }

}

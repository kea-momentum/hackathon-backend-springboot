package com.momentum.releaser.domain.release.dto;

import java.util.Date;
import java.util.List;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import lombok.*;

import com.querydsl.core.annotations.QueryProjection;

import com.momentum.releaser.domain.release.domain.ReleaseEnum.ReleaseDeployStatus;

public class ReleaseDataDto {

    /**
     * 5.1 프로젝트별 릴리즈 노트 목록 조회
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ReleasesDataDTO {
        private Long releaseId;
        private String version;
        private String summary;
        private Date deployDate;
        private ReleaseDeployStatus deployStatus;
        private Double coordX;
        private Double coordY;

        @Builder
        public ReleasesDataDTO(Long releaseId, String version, String summary, Date deployDate, ReleaseDeployStatus deployStatus, Double coordX, Double coordY) {
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
     * 6.3 릴리즈 노트 의견 목록 조회
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ReleaseOpinionsDataDTO {
        private Long opinionId;
        private String opinion;
        private Long memberId;
        private String memberName;
        private String memberImg;
        private char deleteYN;

        @QueryProjection
        @Builder
        public ReleaseOpinionsDataDTO(Long opinionId, String opinion, Long memberId, String memberName, String memberImg) {
            this.opinionId = opinionId;
            this.opinion = opinion;
            this.memberId = memberId;
            this.memberName = memberName;
            this.memberImg = memberImg;
        }

        /**
         * 해당 사용자가 릴리즈 노트 의견을 삭제할 수 있는지 아닌지를 알려주는 값을 업데이트한다.
         */
        public void updateDeleteYN(char deleteYN) {
            this.deleteYN = deleteYN;
        }
    }

    /**
     * 5.5 릴리즈 노트 조회
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ReleaseApprovalsDataDTO {
        private Long memberId;
        private String memberName;
        private String memberImg;
        private char position;
        private char approval;

        @Builder
        public ReleaseApprovalsDataDTO(Long memberId, String memberName, String memberImg, char position, char approval) {
            this.memberId = memberId;
            this.memberName = memberName;
            this.memberImg = memberImg;
            this.position = position;
            this.approval = approval;
        }
    }

    /**
     * 5.7 릴리즈 노트 그래프 좌표 추가
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class CoordinateDataDTO {
        @NotNull(message = "릴리즈 식별 번호는 1 이상의 숫자여야 합니다.")
        @Min(value = 1, message = "릴리즈 식별 번호는 1 이상의 숫자여야 합니다.")
        private Long releaseId;

        @NotNull(message = "x 좌표를 입력해 주세요.")
        private Double coordX;

        @NotNull(message = "y 좌표를 입력해 주세요.")
        private Double coordY;

        @Builder
        public CoordinateDataDTO(Long releaseId, Double coordX, Double coordY) {
            this.releaseId = releaseId;
            this.coordX = coordX;
            this.coordY = coordY;
        }
    }

    /**
     * 9.1 프로젝트별 릴리즈 보고서 조회
     */
    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class GetTagsDataDTO {
        private String tag;
        private List<GetIssueTitleDataDTO> titleList;

        @Builder
        public GetTagsDataDTO(String tag, List<GetIssueTitleDataDTO> titleList) {
            this.tag = tag;
            this.titleList = titleList;
        }
    }

    /**
     * 9.1 프로젝트별 릴리즈 보고서 조회
     */
    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class GetIssueTitleDataDTO {
        private Long issueId;
        private String title;
        private String summary;

        @Builder
        public GetIssueTitleDataDTO(Long issueId, String title, String summary) {
            this.issueId = issueId;
            this.title = title;
            this.summary = summary;
        }
    }
}

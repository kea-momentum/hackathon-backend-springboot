package com.momentum.releaser.domain.project.dto;

import javax.validation.constraints.*;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

public class ProjectRequestDto {

    /**
     * 프로젝트 정보
     */
    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ProjectInfoRequestDTO {

        @NotBlank(message = "프로젝트명은 공백일 수 없습니다.")
        @Size(min = 1, max = 45, message = "프로젝트명은 1자 이상 45자 이하여야 합니다.")
        private String title;

        @NotBlank(message = "프로젝트 설명은 공백일 수 없습니다.")
        @Size(min = 1, max = 100, message = "프로젝트 설명은 1자 이상 100자 이하여야 합니다.")
        private String content;

        @NotBlank(message = "팀명은 공백일 수 없습니다.")
        private String team;

        private String img;

        @Builder
        public ProjectInfoRequestDTO(String title,String content, String team, String img) {
            this.title = title;
            this.content = content;
            this.team = team;
            this.img = img == null ? "" : img;
        }
    }

    /**
     * requestParam - 이슈 그룹
     */
    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class FilterIssueRequestDTO {
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private Date startDate;

        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private Date endDate;

        @Positive(message = "담당자 식별 번호는 양수만 가능합니다.")
        private Long managerId;

        @Pattern(regexp = "^(?!0)\\d+\\.\\d+\\.\\d+$", message = "릴리즈 버전 형식에 맞지 않습니다.")
        private String startReleaseVersion;
        @Pattern(regexp = "^(?!0)\\d+\\.\\d+\\.\\d+$", message = "릴리즈 버전 형식에 맞지 않습니다.")
        private String endReleaseVersion;

        @Pattern(regexp = "(?i)^(DEPRECATED|CHANGED|NEW|FEATURE|FIXED)$", message = "태그 타입은 DEPRECATED, CHANGED, NEW, FEATURE, FIXED 중 하나여야 합니다.")
        private String tag;

        private String issueTitle;

        @Builder
        public FilterIssueRequestDTO(Date startDate, Date endDate, Long managerId, String startReleaseVersion, String endReleaseVersion, String tag, String issueTitle) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.managerId = managerId;
            this.startReleaseVersion = startReleaseVersion;
            this.endReleaseVersion = endReleaseVersion;
            this.tag = tag;
            this.issueTitle = issueTitle;
        }
    }

    /**
     * requestParam - 릴리즈 그룹
     */
    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class FilterReleaseRequestDTO {
        @Pattern(regexp = "^(?!0)\\d+\\.\\d+\\.\\d+$", message = "릴리즈 버전 형식에 맞지 않습니다.")
        private String startVersion;
        @Pattern(regexp = "^(?!0)\\d+\\.\\d+\\.\\d+$", message = "릴리즈 버전 형식에 맞지 않습니다.")
        private String endVersion;

        private String releaseTitle;

        @Builder
        public FilterReleaseRequestDTO(String startVersion, String endVersion, String releaseTitle) {
            this.startVersion = startVersion;
            this.endVersion = endVersion;
            this.releaseTitle = releaseTitle;
        }
    }

}

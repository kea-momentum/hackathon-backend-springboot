package com.momentum.releaser.domain.release.dto;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.*;

import lombok.*;

import com.momentum.releaser.domain.release.dto.ReleaseDataDto.CoordinateDataDTO;

public class ReleaseRequestDto {

    /**
     * 5.2 릴리즈 노트 생성
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ReleaseCreateRequestDTO {
        @Size(min = 1, max = 45, message = "릴리즈 제목은 1자 이상 45자 이하여야 합니다.")
        private String title;

        @NotNull(message = "릴리즈 버전 타입을 선택해 주세요.")
        @Pattern(regexp = "(?i)^(MAJOR|MINOR|PATCH)$", message = "버전 타입은 MAJOR, MINOR, PATCH 중 하나여야 합니다.")
        private String versionType;

        @Size(max = 1000, message = "릴리즈 설명은 1000자를 넘을 수 없습니다.")
        private String content;

        @Size(max = 100, message = "릴리즈 요약은 100자를 넘을 수 없습니다.")
        private String summary;

        @NotNull(message = "릴리즈 노트의 x 좌표를 입력해 주세요.")
        private Double coordX;

        @NotNull(message = "릴리즈 노트의 y 좌표를 입력해 주세요.")
        private Double coordY;

        List<Long> issues;

        @Builder
        public ReleaseCreateRequestDTO(String title, String versionType, String content, String summary, Double coordX, Double coordY, List<Long> issues) {
            this.title = title;
            this.versionType = versionType;
            this.content = content;
            this.summary = summary;
            this.coordX = coordX;
            this.coordY = coordY;
            this.issues = issues;
        }
    }

    /**
     * 5.3 릴리즈 노트 수정
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ReleaseUpdateRequestDTO {
        @Size(min = 1, max = 45, message = "릴리즈 제목은 1자 이상 45자 이하여야 합니다.")
        private String title;

        @NotNull(message = "릴리즈 버전을 입력해 주세요.")
        @Pattern(regexp = "^(?!0)\\d+\\.\\d+\\.\\d+$", message = "릴리즈 버전 형식에 맞지 않습니다.")
        private String version;

        @Size(max = 1000, message = "릴리즈 설명은 1000자를 넘을 수 없습니다.")
        private String content;

        @Size(max = 100, message = "릴리즈 요약은 100자를 넘을 수 없습니다.")
        private String summary;

        @NotNull(message = "릴리즈 배포 상태를 입력해 주세요.")
        @Pattern(regexp = "(?i)^(PLANNING|DENIED|DEPLOYED)$", message = "배포 상태 값은 PLANNING, DENIED, DEPLOYED 중 하나여야 합니다.")
        private String deployStatus;

        List<Long> issues;

        @Builder
        public ReleaseUpdateRequestDTO(String title, String version, String content, String summary, String deployStatus, List<Long> issues) {
            this.title = title;
            this.version = version;
            this.content = content;
            this.summary = summary;
            this.deployStatus = deployStatus;
            this.issues = issues;
        }
    }

    /**
     * 5.6 릴리즈 노트 배포 동의 여부 선택 (멤버용)
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ReleaseApprovalRequestDTO {
        @Pattern(regexp = "(?i)^[PYN]$", message = "P, Y, N 값 중 하나를 입력해 주세요.")
        private String approval;

        @Builder
        public ReleaseApprovalRequestDTO(String approval) {
            this.approval = approval;
        }
    }

    /**
     * 5.7 릴리즈 노트 그래프 좌표 추가
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ReleaseNoteCoordinateRequestDTO {

        @Valid
        List<CoordinateDataDTO> coordinates;

        @Builder
        public ReleaseNoteCoordinateRequestDTO(List<CoordinateDataDTO> coordinates) {
            this.coordinates = coordinates;
        }
    }

    /**
     * 6.1 릴리즈 노트 의견 추가
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ReleaseOpinionCreateRequestDTO {

        @NotBlank(message = "릴리즈 노트에 대한 의견을 작성해 주세요.")
        private String opinion;

        @Builder
        public ReleaseOpinionCreateRequestDTO(String opinion) {
            this.opinion = opinion;
        }
    }

    /**
     * 9.2 프로젝트별 릴리즈 보고서 수정
     */
    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class UpdateReleaseDocsRequestDTO {
        @NotNull(message = "이슈 식별 번호를 입력해주세요.")
        @Min(value = 1, message = "이슈 식별 번호는 1 이상의 숫자여야 합니다.")
        private Long issueId;

        private String summary;

        @Builder
        public UpdateReleaseDocsRequestDTO(Long issueId, String summary) {
            this.issueId = issueId;
            this.summary = summary;
        }
    }
}

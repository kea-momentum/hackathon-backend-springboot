package com.momentum.releaser.domain.issue.dto;

import java.util.Date;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class IssueRequestDto {

    /**
     * 이슈 정보 - 생성, 수정
     */
    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class IssueInfoRequestDTO {

        @NotBlank
        @NotNull(message = "이슈명을 입력해주세요.")
        @Size(min = 1, max = 45)
        private String title;

        @NotBlank
        @NotNull(message = "이슈 설명을 입력해주세요.")
        @Size(min = 1, max = 500)
        private String content;

        @NotNull(message = "태그를 선택해주세요.")
        @Pattern(regexp = "(?i)^(DEPRECATED|CHANGED|NEW|FEATURE|FIXED)$", message = "태그는 DEPRECATED, CHANGED, NEW, FEATURE, FIXED 중 하나여야 합니다.")
        private String tag;

        private Date endDate;
        private Long memberId;

        @Builder
        public IssueInfoRequestDTO(String title, String content, String tag, Date endDate, Long memberId) {
            this.title = title;
            this.content = content;
            this.tag = tag;
            this.endDate = endDate;
            this.memberId = memberId;
        }
    }

    /**
     * 이슈 의견 정보 - 추가
     */
    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class RegisterOpinionRequestDTO {
        @NotBlank
        @NotNull(message = "의견을 입력해주세요.")
        @Size(min = 1, max = 300)
        private String opinion;

        @Builder
        public RegisterOpinionRequestDTO(String opinion) {
            this.opinion = opinion;
        }
    }

}

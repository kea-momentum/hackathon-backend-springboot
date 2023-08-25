package com.momentum.releaser.domain.project.dto;

import java.util.List;

import com.momentum.releaser.domain.project.dto.ProjectDataDto.GetIssueInfoDataDTO;
import com.momentum.releaser.domain.project.dto.ProjectDataDto.GetReleaseInfoDataDTO;
import lombok.*;

import com.momentum.releaser.domain.project.dto.ProjectDataDto.GetProjectDataDTO;

public class ProjectResponseDto {

    /**
     * 프로젝트 정보
     */
    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ProjectInfoResponseDTO {
        private Long projectId;

        @Builder
        public ProjectInfoResponseDTO(Long projectId) {
            this.projectId = projectId;
        }
    }

    /**
     * 프로젝트 조회
     */
    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class GetProjectResponseDTO {
        private List<GetProjectDataDTO> getCreateProjectList;
        private List<GetProjectDataDTO> getEnterProjectList;

        @Builder
        public GetProjectResponseDTO(List<GetProjectDataDTO> getCreateProjectList, List<GetProjectDataDTO> getEnterProjectList) {
            this.getCreateProjectList = getCreateProjectList;
            this.getEnterProjectList = getEnterProjectList;
        }
    }

    /**
     * 검색 후 응답
     */
    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ProjectSearchResponseDTO {
        private List<GetReleaseInfoDataDTO> getReleaseInfoList;
        private List<GetIssueInfoDataDTO> getIssueInfoList;

        @Builder
        public ProjectSearchResponseDTO(List<GetReleaseInfoDataDTO> getReleaseInfoList, List<GetIssueInfoDataDTO> getIssueInfoList) {
            this.getReleaseInfoList = getReleaseInfoList;
            this.getIssueInfoList = getIssueInfoList;
        }
    }

}

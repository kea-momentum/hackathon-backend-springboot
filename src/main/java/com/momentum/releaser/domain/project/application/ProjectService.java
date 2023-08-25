package com.momentum.releaser.domain.project.application;

import java.io.IOException;

import com.momentum.releaser.domain.project.dto.ProjectRequestDto;
import com.momentum.releaser.domain.project.dto.ProjectRequestDto.FilterIssueRequestDTO;
import com.momentum.releaser.domain.project.dto.ProjectRequestDto.FilterReleaseRequestDTO;
import com.momentum.releaser.domain.project.dto.ProjectRequestDto.ProjectInfoRequestDTO;
import com.momentum.releaser.domain.project.dto.ProjectResponseDto;
import com.momentum.releaser.domain.project.dto.ProjectResponseDto.GetProjectResponseDTO;
import com.momentum.releaser.domain.project.dto.ProjectResponseDto.ProjectInfoResponseDTO;
import com.momentum.releaser.domain.project.dto.ProjectResponseDto.ProjectSearchResponseDTO;

/**
 * 프로젝트와 관련된 기능을 제공하는 인터페이스입니다.
 */
public interface ProjectService {

    /**
     * 3.1 프로젝트 생성
     */
    ProjectInfoResponseDTO addProject(String email, ProjectInfoRequestDTO projectInfoReq) throws IOException;

    /**
     * 3.2 프로젝트 수정
     */
    ProjectInfoResponseDTO modifyProject(Long projectId, String email, ProjectInfoRequestDTO projectInfoReq) throws IOException;

    /**
     * 3.3 프로젝트 삭제
     */
    String removeProject(Long projectId);

    /**
     * 3.4 프로젝트 조회
     */
    GetProjectResponseDTO findProjects(String email);

    /**
     * 10.1 프로젝트 내 통합 검색
     */
    ProjectSearchResponseDTO findProjectSearch(Long projectId, String filterType, FilterIssueRequestDTO filterIssueGroup, FilterReleaseRequestDTO filterReleaseGroup);
}

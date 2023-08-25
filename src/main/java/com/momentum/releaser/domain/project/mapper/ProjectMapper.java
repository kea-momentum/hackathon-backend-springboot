package com.momentum.releaser.domain.project.mapper;

import com.momentum.releaser.domain.project.dto.ProjectDataDto;
import com.momentum.releaser.domain.project.dto.ProjectDataDto.GetIssueInfoDataDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import com.momentum.releaser.domain.project.domain.Project;
import com.momentum.releaser.domain.project.domain.ProjectMember;
import com.momentum.releaser.domain.project.dto.ProjectResponseDto.ProjectInfoResponseDTO;
import com.momentum.releaser.domain.release.dto.ReleaseResponseDto.ReleasesResponseDTO;
import com.momentum.releaser.domain.release.mapper.ReleaseMapper;

import java.util.List;


@Mapper(uses = ReleaseMapper.class)
public interface ProjectMapper {

    ProjectMapper INSTANCE = Mappers.getMapper(ProjectMapper.class);

    /**
     * Entity(Project) -> DTO(ProjectInfoRes)
     */
    ProjectInfoResponseDTO toProjectInfoRes(Project project);

    /**
     * Entity(Project) -> DTO(ReleasesResponseDto)
     */
    ReleasesResponseDTO toReleasesResponseDto(Project project, ProjectMember member);

}

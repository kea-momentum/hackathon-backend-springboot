package com.momentum.releaser.domain.project.mapper;

import org.mapstruct.factory.Mappers;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.momentum.releaser.domain.project.domain.ProjectMember;
import com.momentum.releaser.domain.project.dto.ProjectMemberDataDto.ProjectMemberInfoDTO;
import com.momentum.releaser.domain.project.dto.ProjectMemberDataDto.ProjectMembersDataDTO;

@Mapper
public interface ProjectMemberMapper {

    ProjectMemberMapper INSTANCE = Mappers.getMapper(ProjectMemberMapper.class);

    /**
     * Entity(ProjectMember) -> DTO(ProjectMembersDataDTO)
     */
    @Mapping(target = "name", source = "projectMember.user.name")
    @Mapping(target = "profileImg", source = "projectMember.user.img")
    ProjectMembersDataDTO toProjectMembersDataDto(ProjectMember projectMember);

    /**
     * Entity(ProjectMember) -> DTO(ProjectMemberInfoDTO)
     */
    @Mapping(target = "position", source = "projectMember.position")
    @Mapping(target = "userId", source = "projectMember.user.userId")
    @Mapping(target = "name", source = "projectMember.user.name")
    @Mapping(target = "img", source = "projectMember.user.img")
    ProjectMemberInfoDTO toGetMembersRes(ProjectMember projectMember);

}

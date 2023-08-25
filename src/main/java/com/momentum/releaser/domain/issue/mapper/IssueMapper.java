package com.momentum.releaser.domain.issue.mapper;

import java.util.List;

import com.momentum.releaser.domain.issue.dto.IssueDataDto;
import com.momentum.releaser.domain.issue.dto.IssueDataDto.IssueDetailsDataDTO;
import com.momentum.releaser.domain.issue.dto.IssueResponseDto;
import com.momentum.releaser.domain.issue.dto.IssueResponseDto.IssueModifyResponseDTO;
import com.momentum.releaser.domain.project.domain.ProjectMember;
import com.momentum.releaser.domain.project.dto.ProjectDataDto;
import com.momentum.releaser.domain.project.dto.ProjectDataDto.GetIssueInfoDataDTO;
import com.momentum.releaser.domain.project.dto.ProjectDataDto.GetMembersDataDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import com.momentum.releaser.domain.issue.domain.Issue;
import com.momentum.releaser.domain.issue.dto.IssueDataDto.ConnectedIssuesDataDTO;
import com.momentum.releaser.domain.issue.dto.IssueResponseDto.IssueDetailsDTO;
import com.momentum.releaser.domain.issue.dto.IssueResponseDto.OpinionInfoResponseDTO;
import com.momentum.releaser.domain.project.dto.ProjectDataDto.GetMembersDataDTO;
import com.momentum.releaser.domain.project.mapper.ProjectMapper;

@Mapper(uses = { ProjectMapper.class })
public interface IssueMapper {

    IssueMapper INSTANCE = Mappers.getMapper(IssueMapper.class);

    /**
     * Entity (Issue) -> DTO(ConnectedIssuesDataDto)
     */
    @Mapping(target = "memberId", source = "issue.member.memberId")
    @Mapping(target = "memberName", source = "issue.member.user.name")
    @Mapping(target = "memberImg", source = "issue.member.user.img")
    @Mapping(target = "issueNum", source = "issue.issueNum.issueNum")
    ConnectedIssuesDataDTO toConnectedIssuesDataDto(Issue issue);

    /**
     * Entity (Issue), DTO(GetMembers, OpinionInfoResponseDTO) -> DTO(ConnectedIssuesDataDto)
     */
    @Mapping(target = "issueNum", source = "issue.issueNum.issueNum")
    @Mapping(target = "manager", source = "issue.member.memberId")
    @Mapping(target = "memberList", source = "memberRes")
    @Mapping(target = "opinionList", source = "opinionRes")
    IssueDetailsDataDTO mapToGetIssue(Issue issue, List<GetMembersDataDTO> memberRes, List<OpinionInfoResponseDTO> opinionRes);

    /**
     * Entity (ProjectMember) -> DTO(IssueModifyResponseDTO)
     */
    IssueModifyResponseDTO toIssueModifyResponseDTO(ProjectMember projectMember);


    @Mapping(target = "releaseVersion", source = "issue.release.version")
    @Mapping(target = "manager", source = "issue.member.memberId")
    @Mapping(target = "managerName", source = "issue.member.user.name")
    @Mapping(target = "managerImg", source = "issue.member.user.img")
    GetIssueInfoDataDTO toGetIssueInfoDataDTO(Issue issue);
}

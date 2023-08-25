package com.momentum.releaser.domain.release.mapper;

import java.util.List;

import com.momentum.releaser.domain.project.domain.ProjectMember;
import com.momentum.releaser.domain.project.dto.ProjectDataDto;
import com.momentum.releaser.domain.project.dto.ProjectDataDto.GetReleaseInfoDataDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import com.momentum.releaser.domain.issue.mapper.IssueMapper;
import com.momentum.releaser.domain.project.mapper.ProjectMemberMapper;
import com.momentum.releaser.domain.release.domain.ReleaseApproval;
import com.momentum.releaser.domain.release.domain.ReleaseNote;
import com.momentum.releaser.domain.release.domain.ReleaseOpinion;
import com.momentum.releaser.domain.release.dto.ReleaseDataDto.ReleaseApprovalsDataDTO;
import com.momentum.releaser.domain.release.dto.ReleaseDataDto.ReleaseOpinionsDataDTO;
import com.momentum.releaser.domain.release.dto.ReleaseDataDto.ReleasesDataDTO;
import com.momentum.releaser.domain.release.dto.ReleaseResponseDto.*;

@Mapper(uses = {ReleaseMapper.class, IssueMapper.class, ProjectMemberMapper.class})
public interface ReleaseMapper {

    ReleaseMapper INSTANCE = Mappers.getMapper(ReleaseMapper.class);

    /**
     * Entity (ReleaseNote) -> DTO (ReleasesDataDto)
     */
    ReleasesDataDTO toReleasesDataDto(ReleaseNote releaseNote);

    /**
     * Entity (ReleaseNote) -> DTO(ReleaseCreateAndUpdateResponseDto)
     */
    ReleaseCreateAndUpdateResponseDTO toReleaseCreateAndUpdateResponseDto(ReleaseNote releaseNote);

    /**
     * Entity (ReleaseNote) -> DTO(ReleaseInfoResponseDto)
     */
    @Mapping(target = "opinions", source = "releaseOpinionsDataDtos")
    ReleaseInfoResponseDTO toReleaseInfoResponseDto(ReleaseNote releaseNote, List<ReleaseOpinionsDataDTO> releaseOpinionsDataDtos);

    /**
     * Entity(ReleaseApproval) -> DTO(ReleaseApprovalsDataDto)
     */
    @Mapping(target = "memberId", source = "releaseApproval.member.memberId")
    @Mapping(target = "memberName", source = "releaseApproval.member.user.name")
    @Mapping(target = "memberImg", source = "releaseApproval.member.user.img")
    @Mapping(target = "position", source = "releaseApproval.member.position")
    ReleaseApprovalsDataDTO toReleaseApprovalsDataDto(ReleaseApproval releaseApproval);

    /**
     * Entity(ReleaseApproval) -> DTO(ReleaseApprovalsResponseDto)
     */
    @Mapping(target = "memberId", source = "releaseApproval.member.memberId")
    @Mapping(target = "memberName", source = "releaseApproval.member.user.name")
    @Mapping(target = "memberImg", source = "releaseApproval.member.user.img")
    @Mapping(target = "position", source = "releaseApproval.member.position")
    ReleaseApprovalsResponseDTO toReleaseApprovalsResponseDto(ReleaseApproval releaseApproval);

    /**
     * Entity(ReleaseOpinion) -> DTO(ReleaseOpinionCreateResponseDto)
     */
    ReleaseOpinionCreateResponseDTO toReleaseOpinionCreateResponseDto(ReleaseOpinion releaseOpinion);

    /**
     * Entity(ReleaseOpinion) -> DTO(ReleaseOpinionsDataDto)
     */
    @Mapping(target = "memberId", source = "releaseOpinion.member.memberId")
    @Mapping(target = "memberName", source = "releaseOpinion.member.user.name")
    @Mapping(target = "memberImg", source = "releaseOpinion.member.user.img")
    @Mapping(target = "opinionId", source = "releaseOpinion.releaseOpinionId")
    ReleaseOpinionsResponseDTO toReleaseOpinionsResponseDto(ReleaseOpinion releaseOpinion);

    @Mapping(target = "pmId", source = "member.memberId")
    @Mapping(target = "pmName", source = "member.user.name")
    @Mapping(target = "pmImg", source = "member.user.img")
    GetReleaseInfoDataDTO toGetReleaseInfoDataDTO(ReleaseNote release, ProjectMember member);
}

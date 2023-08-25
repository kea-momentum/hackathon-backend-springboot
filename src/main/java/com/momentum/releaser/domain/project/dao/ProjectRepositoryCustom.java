package com.momentum.releaser.domain.project.dao;

import java.util.List;

import com.momentum.releaser.domain.project.domain.Project;
import com.momentum.releaser.domain.project.domain.ProjectMember;
import com.momentum.releaser.domain.project.dto.ProjectDataDto.GetMembersDataDTO;

public interface ProjectRepositoryCustom {

    // 프로젝트에 속한 멤버들의 정보를 List 형태로 반환
    List<GetMembersDataDTO> getMemberList(Project project);

    ProjectMember getProjectMemberPostionPM(Long projectId);
}

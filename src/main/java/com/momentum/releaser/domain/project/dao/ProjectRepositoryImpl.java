package com.momentum.releaser.domain.project.dao;

import java.util.List;

import com.momentum.releaser.domain.project.domain.ProjectMember;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.querydsl.jpa.impl.JPAQueryFactory;

import com.momentum.releaser.domain.project.domain.Project;
import com.momentum.releaser.domain.project.domain.QProjectMember;
import com.momentum.releaser.domain.project.dto.ProjectDataDto.GetMembersDataDTO;
import com.momentum.releaser.domain.project.dto.QProjectDataDto_GetMembersDataDTO;
import com.momentum.releaser.domain.user.domain.QUser;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ProjectRepositoryImpl implements ProjectRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 프로젝트에 속한 멤버들의 정보를 조회
     *
     * @author chaeanna
     * @date 2023-07-04
     * @param project 멤버들을 조회할 프로젝트 엔티티
     * @return List<GetMembers> 프로젝트에 속한 멤버들의 정보 리스트
     */
    @Override
    public List<GetMembersDataDTO> getMemberList(Project project) {
        QProjectMember member = QProjectMember.projectMember;
        QUser user = QUser.user;

        return queryFactory
                .select(new QProjectDataDto_GetMembersDataDTO(
                        member.memberId,
                        user.userId,
                        user.name,
                        user.img,
                        member.position
                ))
                .from(member)
                .leftJoin(member.user, user)
                .where(member.project.eq(project))
                .fetch();
    }

    @Override
    public ProjectMember getProjectMemberPostionPM(Long projectId) {
        QProjectMember member = QProjectMember.projectMember;

        ProjectMember memberRes = queryFactory
                .select(member)
                .from(member)
                .where(member.project.projectId.eq(projectId)
                        .and(member.position.eq('L')))
                .fetchOne();
        return memberRes;
    }
}

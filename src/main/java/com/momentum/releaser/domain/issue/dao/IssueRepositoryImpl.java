package com.momentum.releaser.domain.issue.dao;

import java.util.List;
import java.util.Optional;

import com.momentum.releaser.domain.issue.dto.QIssueResponseDto_ConnectionIssuesResponseDTO;
import com.momentum.releaser.domain.issue.dto.QIssueResponseDto_DoneIssuesResponseDTO;
import com.momentum.releaser.domain.issue.dto.QIssueResponseDto_IssueInfoResponseDTO;
import com.momentum.releaser.domain.issue.dto.QIssueResponseDto_OpinionInfoResponseDTO;
import com.querydsl.core.types.dsl.NumberTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import com.momentum.releaser.domain.issue.domain.*;
import com.momentum.releaser.domain.issue.dto.IssueResponseDto.ConnectionIssuesResponseDTO;
import com.momentum.releaser.domain.issue.dto.IssueResponseDto.DoneIssuesResponseDTO;
import com.momentum.releaser.domain.issue.dto.IssueResponseDto.IssueInfoResponseDTO;
import com.momentum.releaser.domain.issue.dto.IssueResponseDto.OpinionInfoResponseDTO;
import com.momentum.releaser.domain.project.domain.Project;
import com.momentum.releaser.domain.project.domain.QProjectMember;
import com.momentum.releaser.domain.release.domain.QReleaseNote;
import com.momentum.releaser.domain.release.domain.ReleaseNote;
import com.momentum.releaser.domain.user.domain.QUser;

@Slf4j
@Repository
@RequiredArgsConstructor
public class IssueRepositoryImpl implements IssueRepositoryCustom{
    private final JPAQueryFactory queryFactory;

    /**
     * 프로젝트의 마지막 이슈 번호 조회
     *
     * @author chaeanna
     * @date 2023-07-08
     * @param project 프로젝트 정보
     * @return Long 프로젝트의 마지막 이슈 번호
     */
    @Override
    public Long getIssueNum(Project project) {
        QIssueNum issueNum = QIssueNum.issueNum1;

        // 이슈 번호 중 가장 큰 값을 조회
        Optional<Long> result = Optional.ofNullable(queryFactory
                .select(issueNum.issueNum.max())
                .from(issueNum)
                .where(
                        issueNum.project.eq(project)
                )
                .limit(1)
                .fetchOne()
        );

        // 해당 값을 반환하고, 없으면 기본값인 0 반환
        Long number = 0L;
        if (result.isPresent()) {
            number = result.get();
        }

        return number;
    }

    /**
     * 이슈 번호가 없는 데이터 삭제
     *
     * @author chaeanna
     * @date 2023-07-08
     */
    @Override
    public void deleteByIssueNum() {
        QIssueNum issueNum = QIssueNum.issueNum1;

        // 이슈 번호가 없거나 프로젝트 정보가 없는 데이터 삭제
        queryFactory
                .delete(issueNum)
                .where(issueNum.project.isNull()
                        .or(issueNum.issue.isNull()))
                .execute();
    }

    /**
     * 프로젝트에 속하는 모든 이슈 조회
     *
     * @author chaeanna
     * @date 2023-07-07
     * @param getProject 프로젝트 정보
     * @return IssueInfoResponseDTO 프로젝트에 속하는 모든 이슈 정보
     */
    @Override
    public List<IssueInfoResponseDTO> getIssues(Project getProject) {
        QIssue issue = QIssue.issue;
        QProjectMember member = QProjectMember.projectMember;
        QUser user = QUser.user;
        QReleaseNote releaseNote = QReleaseNote.releaseNote;

        // 주어진 프로젝트에 속하는 모든 이슈 정보 조회
        List<IssueInfoResponseDTO> result = queryFactory
                .select(new QIssueResponseDto_IssueInfoResponseDTO(
                        issue.issueId,
                        issue.issueNum.issueNum,
                        issue.title,
                        issue.content,
                        issue.endDate,
                        member.memberId,
                        user.name.as("memberName"),
                        user.img.as("memberImg"),
                        Expressions.stringTemplate("CAST({0} AS string)", issue.tag),
                        releaseNote.version.as("releaseVersion"),
                        issue.edit,
                        Expressions.stringTemplate("CAST({0} AS string)", issue.lifeCycle))
                )
                .from(issue)
                .leftJoin(issue.member, member)
                .leftJoin(member.user, user)
                .leftJoin(issue.release, releaseNote)
                .where(issue.project.eq(getProject))
                .fetchResults().getResults();

        return result;
    }

    /**
     * 프로젝트에서 완료된 이슈 조회
     *
     * @author chaeanna
     * @date 2023-07-07
     * @param getProject 프로젝트 정보
     * @param status 이슈의 상태
     * @return DoneIssuesResponseDTO 프로젝트에서 완료된 이슈 정보
     */
    @Override
    public List<DoneIssuesResponseDTO> getDoneIssues(Project getProject, String status) {
        QIssue issue = QIssue.issue;
        QProjectMember member = QProjectMember.projectMember;
        QUser user = QUser.user;

        // 주어진 프로젝트에서 완료된 이슈들의 정보 조회
        List<DoneIssuesResponseDTO> getDoneIssues = queryFactory
                .select(new QIssueResponseDto_DoneIssuesResponseDTO(
                        issue.issueId,
                        issue.issueNum.issueNum,
                        issue.title,
                        Expressions.stringTemplate("CAST({0} AS string)", issue.tag),
                        issue.endDate,
                        issue.edit,
                        Expressions.cases().when(member.status.eq('N')).then(0L).otherwise(member.memberId),
                        user.name.as("memberName"),
                        user.img.as("memberImg"))
                )
                .from(issue)
                .leftJoin(issue.member, member)
                .leftJoin(member.user, user)
                .where(issue.project.eq(getProject)
                        .and(issue.lifeCycle.eq(LifeCycle.valueOf(status.toUpperCase())))
                        .and(issue.release.isNull()))
                .fetchResults().getResults();

        return getDoneIssues;
    }

    /**
     * 릴리즈 노트별 연결된 이슈 조회
     *
     * @author chaeanna
     * @date 2023-07-07
     * @param getProject 프로젝트 정보
     * @param getReleaseNote 릴리즈 노트 정보
     * @return ConnectionIssuesResponseDTO 릴리즈 노트에 연결된 이슈 정보
     */
    @Override
    public List<ConnectionIssuesResponseDTO> getConnectionIssues(Project getProject, ReleaseNote getReleaseNote) {
        QIssue issue = QIssue.issue;
        QProjectMember member = QProjectMember.projectMember;
        QUser user = QUser.user;
        QReleaseNote releaseNote = QReleaseNote.releaseNote;

        // 주어진 프로젝트에서 특정 릴리즈 노트에 연결된 이슈들의 정보 조회
        List<ConnectionIssuesResponseDTO> getConnectionIssues = queryFactory
                .select(new QIssueResponseDto_ConnectionIssuesResponseDTO(
                        issue.issueId,
                        issue.issueNum.issueNum,
                        issue.title,
                        Expressions.stringTemplate("CAST({0} AS string)", issue.tag),
                        issue.edit,
                        Expressions.cases().when(member.status.eq('N')).then(0L).otherwise(member.memberId),
                        user.name.as("memberName"),
                        user.img.as("memberImg"),
                        releaseNote.version)
                )
                .from(issue)
                .leftJoin(issue.member, member)
                .leftJoin(member.user, user)
                .leftJoin(issue.release, releaseNote)
                .where(issue.project.eq(getProject)
                        .and(issue.release.eq(getReleaseNote)))
                .fetchResults().getResults();

        return getConnectionIssues;
    }

    /**
     * 이슈별 의견 조회
     *
     * @author chaeanna
     * @date 2023-07-08
     * @param issue 조회할 이슈 정보
     * @return OpinionInfoResponseDTO 이슈에 대한 모든 의견 정보
     */
    @Override
    public List<OpinionInfoResponseDTO> getIssueOpinion(Issue issue) {
        QIssueOpinion issueOpinion = QIssueOpinion.issueOpinion;
        QProjectMember member = QProjectMember.projectMember;
        QUser user = QUser.user;

        // 주어진 이슈에 대한 모든 의견들의 정보 조회
        List<OpinionInfoResponseDTO> opinionInfoRes = queryFactory
                .select(new QIssueResponseDto_OpinionInfoResponseDTO(
                        Expressions.cases().when(issueOpinion.member.status.eq('N')).then(0L).otherwise(issueOpinion.member.memberId),
                        user.name.as("memberName"),
                        user.img.as("memberImg"),
                        issueOpinion.issueOpinionId.as("opinionId"),
                        issueOpinion.opinion
                ))
                .from(issueOpinion)
                .leftJoin(issueOpinion.member, member)
                .leftJoin(issueOpinion.member.user, user)
                .where(issueOpinion.issue.eq(issue))
                .fetchResults().getResults();

        return opinionInfoRes;
    }

    /**
     * FULLTEXT 검색을 이용하여 필터링된 이슈 정보 조회
     *
     * @param booleanTemplate FULLTEXT 검색에 사용할 NumberTemplate
     * @param project 검색할 프로젝트 정보
     * @return FULLTEXT 검색 결과로 필터링된 이슈 정보 리스트
     * @date 2023-08-06
     * @author chaeanna
     */
    @Override
    public List<Issue> getSearch(NumberTemplate booleanTemplate, Project project) {
        QIssue issue = QIssue.issue;
        // FULLTEXT 검색을 적용하여 이슈 정보 조회
        List<Issue> result = queryFactory
                .select(issue)
                .from(issue)
                .where(booleanTemplate.gt(0)
                        .and(issue.project.eq(project))
                )
                .fetch();
        return result;
    }

}

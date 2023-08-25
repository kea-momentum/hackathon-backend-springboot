package com.momentum.releaser.domain.issue.dao;

import java.util.List;

import com.momentum.releaser.domain.issue.domain.Issue;
import com.momentum.releaser.domain.issue.dto.IssueResponseDto.*;
import com.momentum.releaser.domain.project.domain.Project;
import com.momentum.releaser.domain.release.domain.ReleaseNote;
import com.querydsl.core.types.dsl.NumberTemplate;

public interface IssueRepositoryCustom {

    // 프로젝트에 속한 이슈의 정보를 List 형태로 반환
    List<IssueInfoResponseDTO> getIssues(Project project);

    // 프로젝트에 관련한 이슈 번호 조회
    Long getIssueNum(Project project);

    // 이슈 번호 삭제
    void deleteByIssueNum();

    // 프로젝트와 상태에 맞는 이슈 List 형태로 반환
    List<DoneIssuesResponseDTO> getDoneIssues(Project findProject, String status);

    // 프로젝트와 연결된 릴리즈에 맞는 이슈 List 형태로 반환
    List<ConnectionIssuesResponseDTO> getConnectionIssues(Project findProject, ReleaseNote findReleaseNote);

    // 이슈에 속한 의견 목록 List 형태로 반환
    List<OpinionInfoResponseDTO> getIssueOpinion(Issue issue);

    // 이슈를 검색할 시 필터링을 거친 정보를 List 형태로 반환
    List<Issue> getSearch(NumberTemplate booleanTemplate, Project project);

}

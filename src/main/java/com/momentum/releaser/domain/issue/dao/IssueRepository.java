package com.momentum.releaser.domain.issue.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.momentum.releaser.domain.issue.domain.Issue;
import com.momentum.releaser.domain.release.domain.ReleaseNote;

@RepositoryRestResource(collectionResourceRel="issue", path="issue")
public interface IssueRepository extends JpaRepository<Issue, Long>, IssueRepositoryCustom, QuerydslPredicateExecutor<Issue> {

    //릴리즈 기반으로 연결된 이슈 목록 List 형태로 반환
    List<Issue> findByRelease(ReleaseNote note);

}

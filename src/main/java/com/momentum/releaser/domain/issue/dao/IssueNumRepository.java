package com.momentum.releaser.domain.issue.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.momentum.releaser.domain.issue.domain.Issue;
import com.momentum.releaser.domain.issue.domain.IssueNum;

@RepositoryRestResource(collectionResourceRel="issue-num", path="issue-num")

public interface IssueNumRepository extends JpaRepository<IssueNum, Long>{

    // 이슈 기반으로 이슈 번호 조회
    IssueNum findByIssue(Issue issue);

}

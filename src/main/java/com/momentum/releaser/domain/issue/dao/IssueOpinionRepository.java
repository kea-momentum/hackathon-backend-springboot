package com.momentum.releaser.domain.issue.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.momentum.releaser.domain.issue.domain.IssueOpinion;

@RepositoryRestResource(collectionResourceRel="issue-opinion", path="issue-opinion")
public interface IssueOpinionRepository extends JpaRepository<IssueOpinion, Long> {

}

package com.momentum.releaser.domain.release.dao.release;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.momentum.releaser.domain.project.domain.Project;
import com.momentum.releaser.domain.release.domain.ReleaseNote;

@RepositoryRestResource(collectionResourceRel="release-note", path="release-note")
public interface ReleaseRepository extends JpaRepository<ReleaseNote, Long>, ReleaseRepositoryCustom, QuerydslPredicateExecutor<ReleaseNote> {

    List<ReleaseNote> findAllByProject(Project project);
}

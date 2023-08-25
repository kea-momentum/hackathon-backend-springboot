package com.momentum.releaser.domain.release.dao.release;

import java.util.List;

import com.momentum.releaser.domain.project.domain.Project;
import com.momentum.releaser.domain.release.domain.ReleaseNote;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.NumberTemplate;

/**
 * Querydsl을 사용하는 repository
 * @see ReleaseRepositoryImpl
 */
public interface ReleaseRepositoryCustom {

    boolean existsByProjectAndVersion(Project project, Long releaseId, String version);

    List<String> findAllVersionsByProject(Project project);

    List<ReleaseNote> findByProjectAndNotInVersion(Project project, String version);

    List<ReleaseNote> findPreviousReleaseNotes(Project project, String version);

    List<ReleaseNote> getSearch(NumberTemplate booleanTemplate, Project project);

}

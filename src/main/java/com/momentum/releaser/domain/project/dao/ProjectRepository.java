package com.momentum.releaser.domain.project.dao;

import java.util.List;
import java.util.Optional;

import com.momentum.releaser.domain.project.domain.ProjectMember;
import com.momentum.releaser.domain.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.momentum.releaser.domain.project.domain.Project;

@RepositoryRestResource(collectionResourceRel="project", path="project")
public interface ProjectRepository extends JpaRepository<Project, Long>, ProjectRepositoryCustom {

    // 링크를 받아 해당 링크로 등록된 프로젝트 정보를 Optional 형태 반환
    Optional<Project> findByLink(String link);
}

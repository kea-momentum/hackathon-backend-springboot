package com.momentum.releaser.domain.project.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.momentum.releaser.domain.project.domain.Project;
import com.momentum.releaser.domain.project.domain.ProjectMember;
import com.momentum.releaser.domain.user.domain.User;

@RepositoryRestResource(collectionResourceRel="project-member", path="project-member")
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    // 프로젝트를 기반으로 프로젝트 멤버 목록 조회
    List<ProjectMember> findByProject(Project updateProject);

    // 사용자를 기반으로 프로젝트 멤버 목록 조회
    List<ProjectMember> findByUser(User user);

    // 사용자와 프로젝트를 기반으로 프로젝트 멤버 조회
    Optional<ProjectMember> findByUserAndProject(User user, Project project);

    // 사용자와 프로젝트를 기반으로 프로젝트 멤버를 Optional 형태 조회
    Optional<ProjectMember> findOneByUserAndProject(User user, Project project);

}

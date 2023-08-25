package com.momentum.releaser.domain.project.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.sun.istack.NotNull;

import com.momentum.releaser.domain.issue.domain.Issue;
import com.momentum.releaser.domain.issue.domain.IssueNum;
import com.momentum.releaser.domain.project.dto.ProjectRequestDto.ProjectInfoRequestDTO;
import com.momentum.releaser.domain.release.domain.ReleaseNote;
import com.momentum.releaser.global.common.BaseTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE project SET status = 'N' WHERE project_id=?")
@Where(clause = "status = 'Y'")
@Table(name = "project")
@Entity
public class Project extends BaseTime {

    @Id
    @Column(name = "project_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long projectId;

    @NotNull
    @Column(name = "title")
    private String title;

    @NotNull
    @Column(name = "content")
    private String content;

    @NotNull
    @Column(name = "team")
    private String team;

    @Column(name = "img")
    private String img;

    @NotNull
    @Column(name = "link")
    private String link;

    @NotNull
    @Column(name = "status")
    private char status;

    @OneToMany(mappedBy = "project")
    private List<ProjectMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "project")
    private List<ReleaseNote> releases = new ArrayList<>();

    @OneToMany(mappedBy = "project")
    private List<Issue> issues = new ArrayList<>();

    @OneToMany(mappedBy = "project")
    private List<IssueNum> issueNums = new ArrayList<>();

    @Builder
    public Project(Long projectId, String title, String content, String team, String img, String link, char status) {
        this.projectId = projectId;
        this.title = title;
        this.content = content;
        this.team = team;
        this.img = img;
        this.link = link;
        this.status = status;
    }

    /**
     * delete 되기 전 실행된다.
     */
    @PreRemove
    private void preRemove() {
        for (ProjectMember member : members) {
            member.statusToInactive();
        }
        for (ReleaseNote releaseNote : releases) {
            releaseNote.statusToInactive();
            releaseNote.softDelete();
        }
        for (IssueNum issueNum : issueNums){
            issueNum.deleteToProject();
        }
        for (Issue issue : issues) {
            issue.statusToInactive();
            issue.deleteToIssueNum();
            issue.softDelete();
        }
    }

    /**
     * insert 되기 전 (persist 되기전) 실행된다.
     */
    @PrePersist
    public void prePersist() {
        this.status = (this.status == '\0') ? 'Y' : this.status;
    }

    /**
     * 이슈 번호 제거
     */
    public void removeIssueNum(IssueNum issueNum) {
        issueNums.remove(issueNum);
    }

    /**
     * 프로젝트 정보 업데이트
     */
    public void updateProject(ProjectInfoRequestDTO updateReq, String url) {
        this.title = updateReq.getTitle();
        this.content = updateReq.getContent();
        this.team = updateReq.getTeam();
        this.img = url;
    }

    /**
     * 프로젝트 이미지 업데이트
     */
    public void updateImg(String img) {
        this.img = img;
    }

}

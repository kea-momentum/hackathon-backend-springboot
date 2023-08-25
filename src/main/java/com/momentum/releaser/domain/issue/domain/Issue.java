package com.momentum.releaser.domain.issue.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.*;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.sun.istack.NotNull;

import com.momentum.releaser.domain.issue.dto.IssueRequestDto.IssueInfoRequestDTO;
import com.momentum.releaser.domain.project.domain.Project;
import com.momentum.releaser.domain.project.domain.ProjectMember;
import com.momentum.releaser.domain.release.domain.ReleaseNote;
import com.momentum.releaser.domain.release.dto.ReleaseRequestDto.UpdateReleaseDocsRequestDTO;
import com.momentum.releaser.global.common.BaseTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE issue SET status = 'N' WHERE issue_id=?")
@Where(clause = "status = 'Y'")
@Table(name = "issue")
@Entity
public class Issue extends BaseTime {

    @Id
    @Column(name = "issue_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long issueId;

    @NotNull
    @Column(name = "title", columnDefinition = "TEXT")
    private String title;

    @NotNull
    @Column(name = "content")
    private String content;

    @Column(name = "summary")
    private String summary;

    @NotNull
    @Column(name = "tag", columnDefinition = "TEXT")
    @Enumerated(EnumType.STRING)
    private Tag tag;

    @NotNull
    @Column(name = "end_date")
    private Date endDate;

    @NotNull
    @Column(name = "life_cycle")
    @Enumerated(EnumType.STRING)
    private LifeCycle lifeCycle; //이슈 진행 상태

    @NotNull
    @Column(name = "edit")
    private char edit; //수정 여부

    @NotNull
    @Column(name = "status")
    private char status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private ProjectMember member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "release_id")
    private ReleaseNote release;

    @OneToOne
    @JoinColumn(name = "issue_num_id")
    private IssueNum issueNum;

    @OneToMany(mappedBy = "issue")
    private List<IssueOpinion> issueOpinions = new ArrayList<>();

    @Builder
    public Issue(Long issueId, String title, String content, String summary, Tag tag, Date endDate, LifeCycle lifeCycle, char edit, char status, Project project, ProjectMember member, ReleaseNote release, IssueNum issueNum) {
        this.issueId = issueId;
        this.title = title;
        this.content = content;
        this.summary = summary;
        this.tag = tag;
        this.endDate = endDate;
        this.lifeCycle = lifeCycle;
        this.edit = edit;
        this.status = status;
        this.project = project;
        this.member = member;
        this.release = release;
        this.issueNum = issueNum;
    }

    /**
     * delete 되기 전 실행된다.
     */
    @PreRemove
    private void preRemove() {
        deleteToIssueNum();
        for (IssueOpinion opinion : issueOpinions) {
            opinion.statusToInactive();
        }
    }

    /**
     * insert 되기 전 (persist 되기전) 실행된다.
     */
    @PrePersist
    public void prePersist() {
        this.lifeCycle = lifeCycle == null ? LifeCycle.NOT_STARTED : this.lifeCycle;
        this.edit = (this.edit == '\0') ? 'N' : this.edit;
        this.status = (this.status == '\0') ? 'Y' : this.status;
    }

    /**
     * 특정 릴리즈 노트와 이슈를 연결할 때 사용한다.
     */
    public void updateReleaseNote(ReleaseNote releaseNote) {
        this.release = releaseNote;
    }

    /**
     * 이슈 연결을 해제할 때 사용한다.
     */
    public void disconnectReleaseNote() {
        this.release = null;
    }

    /**
     * 이슈 수정
     */
    public void updateIssue(IssueInfoRequestDTO updateReq, char edit, ProjectMember member) {
        this.title = updateReq.getTitle();
        this.content = updateReq.getContent();
        this.edit = edit;
        this.tag = Tag.valueOf(updateReq.getTag().toUpperCase());
        this.endDate = updateReq.getEndDate();
        this.member = member;
    }

    /**
     * 이슈 요약 업데이트
     */
    public void updateSummary(UpdateReleaseDocsRequestDTO updateReq) {
        this.summary = updateReq.getSummary();
    }

    /**
     * 이슈 번호 삭제
     */
    public void deleteToIssueNum() {
        this.issueNum = null;
    }

    /**
     * 삭제를 위한 status = 'N'
     */
    public void statusToInactive() {
        this.status = 'N';
    }

    /**
     * 연관된 이슈 의견 리스트 soft delete
     */
    public void softDelete() {
        for (IssueOpinion opinion : issueOpinions) {
            opinion.statusToInactive();
        }
    }

    /**
     * 이슈 번호 업데이트
     */
    public void updateIssueNum(IssueNum issueNum) {
        this.issueNum = issueNum;
    }

    /**
     * 이슈 수정이 수정 상태 업데이트
     */
    public void updateIssueEdit(char status){
        this.edit = status;
    }

    /**
     * 이슈 상태 변경 업데이트
     */
    public void updateLifeCycle(String lifeCycle) {
        this.lifeCycle = LifeCycle.valueOf(lifeCycle);
    }


}

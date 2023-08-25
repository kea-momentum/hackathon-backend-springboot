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
import com.momentum.releaser.domain.issue.domain.IssueOpinion;
import com.momentum.releaser.domain.release.domain.ReleaseApproval;
import com.momentum.releaser.domain.release.domain.ReleaseOpinion;
import com.momentum.releaser.domain.user.domain.User;
import com.momentum.releaser.global.common.BaseTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE project_member SET status = 'N' WHERE member_id=?")
@Where(clause = "status = 'Y'")
@Table(name = "project_member")
@Entity
public class ProjectMember extends BaseTime {

    @Id
    @Column(name = "member_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberId;

    @NotNull
    @Column(name = "position")
    private char position;

    @NotNull
    @Column(name = "status")
    private char status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @OneToMany(mappedBy = "member")
    private List<Issue> issues = new ArrayList<>();

    @OneToMany(mappedBy = "member")
    private List<ReleaseOpinion> releaseOpinions = new ArrayList<>();

    @OneToMany(mappedBy = "member")
    private List<IssueOpinion> issueOpinions = new ArrayList<>();

    @OneToMany(mappedBy = "member")
    private List<ReleaseApproval> approvals = new ArrayList<>();

    @Builder
    public ProjectMember(Long memberId, char position, char status, User user, Project project) {
        this.memberId = memberId;
        this.position = position;
        this.status = status;
        this.user = user;
        this.project = project;
    }

    /**
     * delete 되기 전 실행된다.
     */
    @PreRemove
    private void preRemove() {

        for (ReleaseApproval approval : approvals) {
            approval.deleteToMember();
        }

    }

    /**
     * insert 되기 전 (persist 되기 전) 실행된다.
     */
    @PrePersist
    public void prePersist() {
        this.status = (this.status == '\0') ? 'Y' : this.status;
    }

    /**
     * 상태를 비활성화 (상태 'N'으로 변경)
     */
    public void statusToInactive() {
        this.status = 'N';
    }

    /**
     * 지정된 ReleaseApproval 제거
     */
    public void removeReleaseApproval(ReleaseApproval approval) {
        approvals.remove(approval);
    }

}

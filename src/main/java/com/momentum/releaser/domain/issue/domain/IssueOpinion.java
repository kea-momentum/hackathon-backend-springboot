package com.momentum.releaser.domain.issue.domain;

import javax.persistence.*;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.sun.istack.NotNull;

import com.momentum.releaser.domain.project.domain.ProjectMember;
import com.momentum.releaser.global.common.BaseTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE issue_opinion SET status = 'N' WHERE issue_opinion_id=?")
@Where(clause = "status = 'Y'")
@Table(name = "issue_opinion")
@Entity
public class IssueOpinion extends BaseTime {

    @Id
    @Column(name = "issue_opinion_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long issueOpinionId;

    @NotNull
    @Column(name = "opinion")
    private String opinion;

    @NotNull
    @Column(name = "status")
    private char status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private ProjectMember member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id")
    private Issue issue;

    @Builder
    public IssueOpinion(Long issueOpinionId, String opinion, char status, ProjectMember member, Issue issue) {
        this.issueOpinionId = issueOpinionId;
        this.opinion = opinion;
        this.status = status;
        this.member = member;
        this.issue = issue;
    }

    /**
     * insert 되기전 (persist 되기전) 실행된다.
     */
    @PrePersist
    public void prePersist() {
        this.status = (this.status == '\0') ? 'Y' : this.status;
    }

    /**
     * 삭제를 위한 status ='N' 변경
     */
    public void statusToInactive() {
        this.status = 'N';
    }

}

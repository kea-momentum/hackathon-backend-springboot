package com.momentum.releaser.domain.release.domain;

import javax.persistence.*;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.momentum.releaser.domain.project.domain.ProjectMember;
import com.momentum.releaser.global.common.BaseTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "release_approval")
@Entity
public class ReleaseApproval extends BaseTime {

    @Id
    @Column(name = "approval_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long approvalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private ProjectMember member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "release_id")
    private ReleaseNote release;

    /**
     * P(Pending): 배포 대기 (Default)
     * Y(Yes): 배포 동의
     * N(No): 배포 거부
     */
    @Column(name = "approval")
    private char approval;

    @Builder
    public ReleaseApproval(ProjectMember member, ReleaseNote release) {
        this.member = member;
        this.release = release;
    }

    /**
     * delete 되기 전 실행된다.
     */
    @PreRemove
    private void preRemove() {
        release.removeReleaseApproval(this);
        member.removeReleaseApproval(this);
    }

    /**
     * 데이터베이스에 초기화/저장되기 전에 자동으로 값을 초기화시킨다.
     */
    @PrePersist
    public void prePersist() {
        this.approval = (this.approval == '\0') ? 'P' : this.approval;
    }

    public void deleteToProject() {
        this.release = null;
        this.member = null;
    }

    public void deleteToMember() {
        this.release = null;
        this.member = null;
    }

    /**
     * 릴리즈 노트 배포 동의 여부
     */
    public void updateApproval(char approval) {
        this.approval = approval;
    }
}

package com.momentum.releaser.domain.release.domain;

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
@SQLDelete(sql = "UPDATE release_opinion SET status = 'N' WHERE release_opinion_id=?")
@Where(clause = "status = 'Y'")
@Table(name = "release_opinion")
@Entity
public class ReleaseOpinion extends BaseTime {

    @Id
    @Column(name = "release_opinion_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long releaseOpinionId;

    @NotNull
    @Column(name = "opinion")
    private String opinion;

    @NotNull
    @Column(name = "status")
    private char status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "release_id")
    private ReleaseNote release;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private ProjectMember member;

    @Builder
    public ReleaseOpinion(String opinion, ReleaseNote release, ProjectMember member) {
        this.opinion = opinion;
        this.release = release;
        this.member = member;
    }

    /**
     * insert 되기전 (persist 되기전) 실행된다.
     */
    @PrePersist
    public void prePersist() {
        this.status = (this.status == '\0') ? 'Y' : this.status;
    }

    public void statusToInactive() {
        this.status = 'N';
    }

}

package com.momentum.releaser.domain.user.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;

import com.momentum.releaser.domain.issue.domain.IssueOpinion;
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
@SQLDelete(sql = "UPDATE user SET status = 'N' WHERE user_id=?")
@Where(clause = "status = 'Y'")
@Table(name = "user")
@Entity
public class User extends BaseTime {

    @Id
    @Column(name = "user_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @NotNull
    @Column(name = "name")
    private String name;

    @NotNull
    @Column(name = "email")
    private String email;

    @Column(name = "img")
    private String img;

    @NotNull
    @Column(name = "status")
    private char status;

    @OneToOne
    @JoinColumn(name = "auth_id")
    private AuthSocial authSocial;

    @OneToOne
    @JoinColumn(name = "security_id")
    private AuthPassword authPassword;

    @OneToMany(mappedBy = "user")
    private List<ProjectMember> members = new ArrayList<>();

    @Builder
    public User(String name, String email, String img, char status) {
        this.name = name;
        this.email = email;
        this.img = img;
        this.status = status;
    }

    /**
     * delete 되기 전 실행된다.
     */
    @PreRemove
    private void preRemove() {
        if (authSocial != null) {
            authSocial.statusToInactive();
        }
        if (authPassword != null) {
            authPassword.statusToInactive();
        }
    }

    /**
     * insert 되기전 (persist 되기전) 실행된다.
     */
    @PrePersist
    public void prePersist() {
        this.status = (this.status == '\0') ? 'Y' : this.status;
    }

    /**
     * 사용자 프로필 이미지를 업데이트한다.
     */
    public void updateImg(String img) {
        this.img = img;
    }

    public void updateAuthPassword(AuthPassword authPassword) {
        this.authPassword = authPassword;
    }
    public void updateAuth(AuthSocial authSocial, AuthPassword authPassword) {
        this.authSocial = authSocial;
        this.authPassword = authPassword;
    }
}

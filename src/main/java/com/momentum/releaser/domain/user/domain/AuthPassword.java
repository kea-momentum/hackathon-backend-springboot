package com.momentum.releaser.domain.user.domain;

import com.momentum.releaser.global.common.BaseTime;
import com.sun.istack.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE auth_password SET status = 'N' WHERE security_id=?")
@Where(clause = "status = 'Y'")
@Table(name = "auth_password")
@Entity
public class AuthPassword extends BaseTime {

    @Id
    @Column(name = "security_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long securityId;

    @NotNull
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @NotNull
    @Column(name = "password")
    private String password;

    @NotNull
    @Column(name = "status")
    private char status;

    @Builder
    public AuthPassword(User user, String password, char status) {
        this.user = user;
        this.password = password;
        this.status = status;
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

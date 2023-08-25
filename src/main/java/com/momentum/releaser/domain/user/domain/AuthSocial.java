package com.momentum.releaser.domain.user.domain;

import com.momentum.releaser.global.common.BaseTime;
import com.momentum.releaser.global.jwt.AuthProvider;
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
@SQLDelete(sql = "UPDATE auth_social SET status = 'N' WHERE auth_id=?")
@Where(clause = "status = 'Y'")
@Table(name = "auth_social")
@Entity
public class AuthSocial extends BaseTime {

    @Id
    @Column(name = "auth_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long authId;

    @NotNull
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @NotNull
    @Enumerated(EnumType.STRING)
    private AuthProvider provider;

    @NotNull
    @Column(name = "status")
    private char status;



    @Builder
    public AuthSocial(User user, AuthProvider provider, char status) {
        this.user = user;
        this.provider = provider;
        this.status = status;
    }

    /**
     * insert 되기전 (persist 되기전) 실행된다.
     */
    @PrePersist
    public void prePersist() {
        this.status = (this.status == '\0') ? 'Y' : this.status;
    }

//    public AuthSocial(User user, AuthProvider authProvider, Object token, char y) {
//        super();
//    }
//    public AuthSocial(User user, AuthProvider authProvider, char y) {
//        super();
//    }

    /**
     * 삭제를 위한 status ='N' 변경
     */
    public void statusToInactive() {
        this.status = 'N';
    }

}

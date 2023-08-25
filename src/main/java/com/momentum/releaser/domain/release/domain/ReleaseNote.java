package com.momentum.releaser.domain.release.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import lombok.*;

import com.sun.istack.NotNull;

import com.momentum.releaser.domain.issue.domain.Issue;
import com.momentum.releaser.domain.project.domain.Project;
import com.momentum.releaser.domain.release.domain.ReleaseEnum.ReleaseDeployStatus;
import com.momentum.releaser.global.common.BaseTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE release_note SET status = 'N' WHERE release_id=?")
@Where(clause = "status = 'Y'")
@Table(name = "release_note")
@Entity
public class ReleaseNote extends BaseTime {

    @Id
    @Column(name = "release_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long releaseId;

    @NotNull
    @NotBlank(message = "릴리즈 제목을 입력해 주세요.")
    @Size(min = 1, max = 45, message = "릴리즈 제목은 1자 이상 45자 이하여야 합니다.")
    @Column(name = "title", columnDefinition = "TEXT")
    private String title;

    @NotNull
    @Size(max = 1000, message = "릴리즈 설명은 1000자를 넘을 수 없습니다.")
    @Column(name = "content")
    private String content;

    @Size(max = 100, message = "릴리즈 요약은 100자를 넘을 수 없습니다.")
    @Column(name = "summary")
    private String summary;

    @NotNull
    @Pattern(regexp = "^(?!0)\\d+\\.\\d+\\.\\d+$", message = "릴리즈 버전 형식에 맞지 않습니다.")
    @Column(name = "version")
    private String version;

    @Column(name = "deploy_date")
    private Date deployDate;

    @NotNull
    @Column(name = "deploy_status")
    @Enumerated(EnumType.STRING)
    private ReleaseDeployStatus deployStatus;

    @NotNull
    @Column(name = "status")
    private char status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "coord_x")
    private Double coordX;

    @Column(name = "coord_y")
    private Double coordY;

    @OneToMany(mappedBy = "release")
    private List<ReleaseOpinion> releaseOpinions = new ArrayList<>();

    @OneToMany(mappedBy = "release")
    private List<Issue> issues = new ArrayList<>();

    @OneToMany(mappedBy = "release")
    private List<ReleaseApproval> approvals = new ArrayList<>();

    @Builder
    public ReleaseNote(Long releaseId, String title, String content, String summary, String version, Date deployDate, ReleaseDeployStatus deployStatus, Project project, Double coordX, Double coordY) {
        this.releaseId = releaseId;
        this.title = title;
        this.content = content;
        this.summary = summary;
        this.version = version;
        this.deployDate = deployDate;
        this.deployStatus = deployStatus;
        this.project = project;
        this.coordX = coordX;
        this.coordY = coordY;
    }

    /**
     * 릴리즈 노트를 삭제하기 전에 연관 관계로 매핑된 엔티티와의 관게를 끊거나 삭제한다.
     */
    @PreRemove
    private void preRemove() {
        // 릴리즈 노트 의견 삭제
        for (ReleaseOpinion opinion : releaseOpinions) {
            opinion.statusToInactive();
        }
    }

    /**
     * insert 되기전 (persist 되기전) 실행된다.
     */
    @PrePersist
    public void prePersist() {
        this.deployStatus = (this.deployStatus == null) ? ReleaseDeployStatus.PLANNING : this.deployStatus;
        this.status = (this.status == '\0') ? 'Y' : this.status;
    }

    /**
     * 연관 관계로 매핑되어 있는 릴리즈 노트의 의견들을 삭제할 때 사용한다.
     */
    public void softDelete() {
        for (ReleaseOpinion opinion : releaseOpinions) {
            opinion.statusToInactive();
        }
        for (ReleaseApproval approval : approvals) {
            approval.deleteToProject();
        }
    }

    public void removeReleaseApproval(ReleaseApproval approval) {
        approvals.remove(approval);
    }

    /**
     * 릴리즈 노트를 삭제할 때 사용한다.
     */
    public void statusToInactive() {
        this.status = 'N';
    }

    /**
     * 릴리즈 노트 정보를 업데이트할 때 사용한다.
     */
    public void updateReleaseNote(String title, String content, String summary, String version, Date deployDate, ReleaseDeployStatus deployStatus) {
        this.title = title;
        this.content = content;
        this.version = version;
        this.summary = summary;
        this.deployDate = deployDate;
        this.deployStatus = deployStatus;
    }

    /**
     * 릴리즈 노트의 x 좌표를 업데이트한다.
     */
    public void updateCoordX(Double coordX) {
        this.coordX = coordX;
    }

    /**
     * 릴리즈 노트의 y 좌표를 업데이트한다.
     */
    public void updateCoordY(Double coordY) {
        this.coordY = coordY;
    }

    /**
     * 릴리즈 x, y 좌표를 업데이트한다.
     */
    public void updateCoordinates(Double coordX, Double coordY) {
        this.coordX = coordX;
        this.coordY = coordY;
    }

    /**
     * 릴리즈 노트 배포 상태를 업데이트한다.
     */
    public void updateDeployStatus(ReleaseDeployStatus deployStatus) {
        this.deployStatus = deployStatus;
    }
}

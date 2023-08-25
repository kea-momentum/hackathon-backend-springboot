package com.momentum.releaser.redis.issue;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueStatus {

    private Long issueId;

    private String lifeCycle;
    private Integer index;

    @Builder
    public IssueStatus(Long issueId, String lifeCycle, Integer index) {
        this.issueId = issueId;
        this.lifeCycle = lifeCycle;
        this.index = index;
    }

    public void updateIndex(Integer index) {
        this.index = index;
    }

    public void updateStatus(IssueStatus issueStatus) {
        this.issueId = issueStatus.getIssueId();
        this.lifeCycle = issueStatus.getLifeCycle();
        this.index = issueStatus.getIndex();
    }
}

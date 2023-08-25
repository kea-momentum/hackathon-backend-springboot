package com.momentum.releaser.redis.issue;

import com.momentum.releaser.domain.issue.application.IssueServiceImpl;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RedisHash(value = "issue")
public class OrderIssue {

    @Id
    private String id;

    @Indexed
    private Long projectId;

    private List<IssueStatus> issueStatusList = new ArrayList<>();

    @Builder
    public OrderIssue(String id, Long projectId, List<IssueStatus> issueStatusList) {
        this.id = id;
        this.projectId = projectId;
        this.issueStatusList = issueStatusList;
    }

    public void updateOrderIndex(IssueStatus issueStatus) {
        if (issueStatusList.contains(issueStatus)) {
            int idx = issueStatusList.indexOf(issueStatus);
            issueStatusList.set(idx, issueStatus);
        } else {
            this.issueStatusList.add(issueStatus);
        }
    }

    public void updateIssueStatus(IssueStatus previousStatus, IssueStatus newStatus) {
        int idx = issueStatusList.indexOf(previousStatus);
        issueStatusList.set(idx, newStatus);
    }

    public void updateIssueStatusList(List<IssueStatus> issueStatusList) {
        this.issueStatusList = issueStatusList;
    }
}

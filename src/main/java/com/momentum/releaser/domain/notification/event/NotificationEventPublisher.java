package com.momentum.releaser.domain.notification.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationEventPublisher {

    private final ApplicationEventPublisher publisher;

    /**
     * 릴리즈 노트 알림 이벤트를 발행한다.
     *
     * @author seonwoo
     * @date 2023-08-14 (월)
     * @param releaseNoteEvent 릴리즈 노트 알림 이벤트
     */
    public void notifyReleaseNote(final ReleaseNoteMessageEvent releaseNoteEvent) {
        publisher.publishEvent(releaseNoteEvent);
    }

    /**
     * 이슈 알림 이벤트를 발행한다.
     *
     * @author seonwoo
     * @date 2023-08-14 (월)
     * @param issueMessageEvent 이슈 알림 이벤트
     */
    public void notifyIssue(final IssueMessageEvent issueMessageEvent) {
        publisher.publishEvent(issueMessageEvent);
    }
}

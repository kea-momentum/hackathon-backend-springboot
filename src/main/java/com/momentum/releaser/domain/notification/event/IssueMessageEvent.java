package com.momentum.releaser.domain.notification.event;

import java.util.List;
import java.util.UUID;

import com.momentum.releaser.rabbitmq.MessageDto.IssueMessageDto;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@Builder
@ToString
public class IssueMessageEvent {
    private String eventId;
    private ConsumerType type;
    private IssueMessageDto message;
    private List<String> consumers;

    /**
     * (사용 X)
     * 이슈와 관련된 알림을 발생시킬 이벤트
     * 모든 프로젝트 멤버에게 알림 전송
     *
     * @param message   알림 메시지
     * @param consumers 알림 소비자(대상) 목록
     * @return IssueMessageEvent
     */
    public static IssueMessageEvent toNotifyAllIssue(final IssueMessageDto message, List<String> consumers) {
        return IssueMessageEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .message(message)
                .type(ConsumerType.PROJECT)
                .consumers(consumers)
                .build();
    }

    /**
     * 이슈와 관련된 알림을 발생시킬 이벤트
     * 개별 사용자에게 알림 전송
     *
     * @param message   알림 메시지
     * @param consumers 알림 소비자(대상) 목록
     * @return IssueMessageEvent
     * @author seonwoo
     * @date 2023-08-14 (월)
     */
    public static IssueMessageEvent toNotifyOneIssue(final IssueMessageDto message, List<String> consumers) {
        return IssueMessageEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .message(message)
                .type(ConsumerType.USER)
                .consumers(consumers)
                .build();
    }
}

package com.momentum.releaser.domain.notification.event;

import java.util.List;
import java.util.UUID;

import com.momentum.releaser.rabbitmq.MessageDto.ReleaseNoteMessageDto;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@Builder
@ToString
public class ReleaseNoteMessageEvent {

    private String eventId;
    private ConsumerType type;
    private ReleaseNoteMessageDto message;
    private List<String> consumers;

    /**
     * (사용 X)
     * 릴리즈 노트와 관련된 알림을 발생시킬 이벤트
     * 단, 이때 알림 대상은 모든 프로젝트 멤버이다.
     *
     * @param message 알림 메시지
     * @return ReleaseNoteMessageEvent
     * @author seonwoo
     * @date 2023-08-09 (수)
     */
    public static ReleaseNoteMessageEvent toNotifyAllReleaseNote(final ReleaseNoteMessageDto message, List<String> consumers) {
        return ReleaseNoteMessageEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .message(message)
                .type(ConsumerType.PROJECT)
                .consumers(consumers)
                .build();
    }

    /**
     * 릴리즈 노트와 관련된 알림을 발생시킬 이벤트
     * 단, 이때 알림 대상은 프로젝트 PM 한 명이다.
     *
     * @param message 알림 메시지
     * @return ReleaseNoteMessageEvent
     * @author seonwoo
     * @date 2023-08-09 (수)
     */
    public static ReleaseNoteMessageEvent toNotifyOneReleaseNote(final ReleaseNoteMessageDto message, List<String> consumers) {
        return ReleaseNoteMessageEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .message(message)
                .type(ConsumerType.USER)
                .consumers(consumers)
                .build();
    }
}

package com.momentum.releaser.rabbitmq;

import com.momentum.releaser.rabbitmq.MessageDto.ReleaserMessageDto;

/**
 * @see MessageServiceImpl
 */
public interface MessageService {

    /**
     * Queue로 사용자 개별 메시지 발행
     *
     * @param userEmail          사용자 이메일
     * @param releaserMessageDto 발행할 메시지의 DTO
     * @author seonwoo
     * @date 2023-08-07 (월)
     */
    void sendMessagePerUser(String userEmail, ReleaserMessageDto releaserMessageDto);

    /**
     * Queue로 사용자 개별 메시지 구독
     *
     * @param releaserMessageDto 구독한 메시지의 DTO
     * @author seonwoo
     * @date 2023-08-07 (월)
     */
    void receiveMessagePerUser(ReleaserMessageDto releaserMessageDto);
}

package com.momentum.releaser.domain.notification.mapper;

import com.momentum.releaser.domain.notification.dto.NotificationResponseDto.NotificationListResponseDto;
import com.momentum.releaser.redis.notification.Notification;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface NotificationMapper {

    NotificationMapper INSTANCE = Mappers.getMapper(NotificationMapper.class);

    /**
     * Redis Entity (Notification) -> DTO (NotificationListResponseDto)
     *
     * @param notification 알림
     * @return NotificationListResponseDto
     * @author seonwoo
     * @date 2023-08-14 (월)
     */
    NotificationListResponseDto toNotificationListResponseDto(Notification notification);
}

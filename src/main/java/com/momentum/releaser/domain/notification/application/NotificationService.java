package com.momentum.releaser.domain.notification.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.momentum.releaser.domain.notification.dto.NotificationRequestDto.NotificationApprovalRequestDto;
import com.momentum.releaser.domain.notification.dto.NotificationResponseDto.NotificationListResponseDto;

public interface NotificationService {

    /**
     * 11.1 사용자별 알림 내역 조회
     *
     * @param userEmail 사용자 이메일
     * @param pageable  페이징을 위한 정보 (예: page, size 등)
     * @return 사용자별 알림 내역 목록
     * @author seonwoo
     * @date 2023-08-14 (월)
     */
    Page<NotificationListResponseDto> findNotificationList(String userEmail, Pageable pageable);

    /**
     * 11.2 릴리즈 노트 배포 동의 선택 알림
     *
     * @param userEmail                      사용자 이메일
     * @param notificationApprovalRequestDto 릴리즈 식별 번호가 담긴 DTO
     * @return 알림 전달 성공 메시지
     * @author seonwoo
     * @date 2023-08-14 (월)
     */
    String sendApprovalNotification(String userEmail, NotificationApprovalRequestDto notificationApprovalRequestDto);

    /**
     * 11.3 알림 읽음 확인
     *
     * @param userEmail      사용자 이메일
     * @param notificationId 알림 식별 문자
     * @return 알림 읽음 업데이트 성공 메시지
     * @author seonwoo
     * @date 2023-08-15 (화)
     */
    String modifyNotificationIsRead(String userEmail, String notificationId);
}

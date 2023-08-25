package com.momentum.releaser.domain.notification.application;

import static com.momentum.releaser.global.config.BaseResponseStatus.*;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.momentum.releaser.domain.notification.dto.NotificationRequestDto.NotificationApprovalRequestDto;
import com.momentum.releaser.domain.notification.dto.NotificationResponseDto.NotificationListResponseDto;
import com.momentum.releaser.domain.notification.event.NotificationEventPublisher;
import com.momentum.releaser.domain.notification.event.ReleaseNoteMessageEvent;
import com.momentum.releaser.domain.notification.mapper.NotificationMapper;
import com.momentum.releaser.domain.project.dao.ProjectMemberRepository;
import com.momentum.releaser.domain.project.domain.Project;
import com.momentum.releaser.domain.release.dao.approval.ReleaseApprovalRepository;
import com.momentum.releaser.domain.release.dao.release.ReleaseRepository;
import com.momentum.releaser.domain.release.domain.ReleaseApproval;
import com.momentum.releaser.domain.release.domain.ReleaseEnum.ReleaseDeployStatus;
import com.momentum.releaser.domain.release.domain.ReleaseNote;
import com.momentum.releaser.global.exception.CustomException;
import com.momentum.releaser.rabbitmq.MessageDto.ReleaseNoteMessageDto;
import com.momentum.releaser.redis.notification.Notification;
import com.momentum.releaser.redis.notification.NotificationPerUser;
import com.momentum.releaser.redis.notification.NotificationPerUserRedisRepository;
import com.momentum.releaser.redis.notification.NotificationRedisRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    // 도메인
    private final ProjectMemberRepository projectMemberRepository;
    private final ReleaseRepository releaseRepository;
    private final ReleaseApprovalRepository releaseApprovalRepository;

    // 알림
    private final NotificationRedisRepository notificationRedisRepository;
    private final NotificationPerUserRedisRepository notificationPerUserRedisRepository;
    private final NotificationEventPublisher notificationEventPublisher;

    /**
     * 11.1 사용자별 알림 내역 조회
     *
     * @param userEmail 사용자 이메일
     * @param pageable  페이징을 위한 정보 (예: page, size 등)
     * @return 사용자별 알림 내역 목록
     * @author seonwoo
     * @date 2023-08-14 (월)
     */
    @Override
    public Page<NotificationListResponseDto> findNotificationList(String userEmail, Pageable pageable) {
        // 사용자의 알림 내역 목록을 페이지네이션해서 가져온다.
        Page<Notification> notifications = findNotificationAllByUserEmail(userEmail, pageable);

        // 페이지네이션한 엔티티 목록을 DTO 목록으로 변환해 가져온다.
        return mapToNotificationListResponseDto(userEmail, notifications, pageable);
    }

    /**
     * 11.2 릴리즈 노트 배포 동의 알림
     *
     * @param userEmail                      사용자 이메일
     * @param notificationApprovalRequestDto 릴리즈 식별 번호가 담긴 DTO
     * @return 릴리즈 노트 배포 동의 알림 전송 성공 메시지
     * @author seonwoo
     * @date 2023-08-14 (월)
     */
    @Transactional(readOnly = true)
    @Override
    public String sendApprovalNotification(String userEmail, NotificationApprovalRequestDto notificationApprovalRequestDto) {
        ReleaseNote releaseNote = findReleaseNoteById(notificationApprovalRequestDto);
        Project project = releaseNote.getProject();

        // 릴리즈 노트 배포 동의 투표가 가능한 상황인지 검증한다.
        validateApprovalNotification(releaseNote);

        // 알림을 위한 릴리즈 노트 유형의 알림 메시지 데이터를 생성한다.
        ReleaseNoteMessageDto message = createReleaseNoteMessage(project, releaseNote);

        // 프로젝트를 이용하여 프로젝트 멤버 이메일 목록을 가져온다.
        List<String> consumers = findProjectMembersByProject(project);

        // 알림 메시지를 전송한다.
        notificationEventPublisher.notifyReleaseNote(ReleaseNoteMessageEvent.toNotifyOneReleaseNote(message, consumers));

        return "릴리즈 노트 배포 동의 여부 알림이 전송되었습니다.";
    }

    /**
     * 11.3 알림 읽음 확인
     *
     * @param userEmail      사용자 이메일
     * @param notificationId 알림 식별 문자
     * @return 알림 읽음 업데이트 성공 메시지
     * @author seonwoo
     * @date 2023-08-15 (화)
     */
    @Override
    public String modifyNotificationIsRead(String userEmail, String notificationId) {
        // Redis에서 해당 알림 정보를 가져온다.
        Notification notification = findNotificationById(notificationId);

        // 알림 읽음 정보를 업데이트한다.
        updateIsReadByUserEmail(userEmail, notification);

        return "알림 읽음 여부 업데이트에 성공하였습니다.";
    }

    // =================================================================================================================

    /**
     * 사용자 이메일을 이용하여 알림 정보를 가져온다.
     *
     * @param userEmail 사용자 이메일
     * @param pageable  페이징을 위한 정보 (예: page, size 등)
     * @return Notification 페이지네이션
     * @author seonwoo
     * @date 2023-08-14 (월)
     */
    private Page<Notification> findNotificationAllByUserEmail(String userEmail, Pageable pageable) {
        // Redis에 저장된 알림 내역을 사용자 이메일을 이용하여 가져온다.
        NotificationPerUser notificationPerUser = notificationPerUserRedisRepository.findById(userEmail)
                .orElseThrow(() -> new CustomException(NOT_EXISTS_NOTIFICATION_PER_USER));

        // 사용자 알림 데이터에 저장된 알림 식별 번호를 가져온다.
        List<String> notificationIds = notificationPerUser.getNotifications();

        // 사용자 알림 식별 번호를 가지고 알림 목록 정보를 가져온다.
        Iterable<Notification> notificationIterable = notificationRedisRepository.findAllById(notificationIds);
        List<Notification> notifications = new ArrayList<>();
        notificationIterable.forEach(notifications::add);

        // 페이지네이션을 적용시킨 목록으로 변환한 후 반환한다.
        return createPageFromList(notifications, pageable);
    }

    /**
     * List 형식의 알림 목록을 Page 형태로 변환한다.
     *
     * @param notifications 알림 목록
     * @param pageable      페이지네이션을 위한 정보 (예: page, size 등)
     * @return Notification 페이지네이션
     * @author seonwoo
     * @date 2023-08-14 (월)
     */
    private Page<Notification> createPageFromList(List<Notification> notifications, Pageable pageable) {
        int startIdx = (int) pageable.getOffset();
        int endIdx = Math.min(startIdx + pageable.getPageSize(), notifications.size());
        List<Notification> notificationsAfterPaging = notifications.subList(startIdx, endIdx);
        return new PageImpl<>(notificationsAfterPaging, pageable, notifications.size());
    }

    /**
     * Entity 목록을 DTO 목록으로 변환
     *
     * @param userEmail     사용자 이메일
     * @param notifications 알림 목록
     * @param pageable      페이지네이션을 위한 정보 (예: page, size 등)
     * @return 반환 DTO로 변환한 알림 목록
     */
    private PageImpl<NotificationListResponseDto> mapToNotificationListResponseDto(String userEmail, Page<Notification> notifications, Pageable pageable) {
        // 전달받은 엔티티 목록을 DTO 목록으로 매핑한다.
        List<NotificationListResponseDto> notificationDtos = notifications.stream()
                .map(NotificationMapper.INSTANCE::toNotificationListResponseDto)
                .collect(Collectors.toList());

        // DTO 필드 중 사용자가 해당 알림을 읽었는지를 나타내는 isRead 값을 업데이트한다.
        for (NotificationListResponseDto notificationDto : notificationDtos) {
            Notification notification = notificationRedisRepository.findById(notificationDto.getNotificationId())
                    .orElseThrow(() -> new CustomException(NOT_EXISTS_NOTIFICATION_PER_USER));

            Map<String, Integer> markByUsers = notification.getMarkByUsers();

            if (markByUsers == null) {
                throw new CustomException(NOT_EXISTS_USERS_IN_NOTIFICATION_DATA);
            }

            Integer isRead = markByUsers.get(userEmail);
            notificationDto.updateIsRead(isRead == null ? 0 : isRead);
        }

        // Page 구현체를 이용하여 목록을 반환한다.
        return new PageImpl<>(notificationDtos, pageable, notifications.getTotalElements());
    }

    /**
     * 릴리즈 노트 식별 번호를 이용하여 릴리즈 노트 엔티티를 반환한다.
     *
     * @param notificationApprovalRequestDto 릴리즈 식별 번호
     * @return ReleaseNote 엔티티
     * @author seonwoo
     * @date 2023-08-14 (월)
     */
    private ReleaseNote findReleaseNoteById(NotificationApprovalRequestDto notificationApprovalRequestDto) {
        Long releaseId = notificationApprovalRequestDto.getReleaseId();
        return releaseRepository.findById(releaseId).orElseThrow(() -> new CustomException(NOT_EXISTS_RELEASE_NOTE));
    }

    /**
     * 릴리즈 노트 배포 동의 투표를 실행할 수 있는 상황인지 검증한다.
     *
     * @param releaseNote 릴리즈 노트 엔티티
     * @author seonwoo
     * @date 2023-08-14 (월)
     */
    private void validateApprovalNotification(ReleaseNote releaseNote) {
        // 만약 이미 배포가 된 릴리즈 노트라면 알림을 전송할 수 없다.
        if (releaseNote.getDeployStatus() == ReleaseDeployStatus.DEPLOYED) {
            throw new CustomException(ALREADY_DEPLOYED_RELEASE_NOTE);
        }

        // 만약 이미 모든 멤버들의 동의가 완료되었다면 알림을 전송할 수 없다.
        List<ReleaseApproval> approvals = releaseApprovalRepository.findAllByRelease(releaseNote);
        int yesCount = 0;

        for (ReleaseApproval approval : approvals) {
            if (approval.getApproval() == 'Y') {
                yesCount++;
            }
        }

        if (yesCount == approvals.size()) {
            throw new CustomException(ALREADY_ALL_APPROVALS_WITH_YES);
        }
    }

    /**
     * 릴리즈 노트 유형의 알림 메시지 생성
     *
     * @param project     프로젝트
     * @param releaseNote 릴리즈 노트
     * @return 릴리즈 노트 유형의 알림 메시지
     * @author seonwoo
     * @date 2023-08-14 (월)
     */
    private ReleaseNoteMessageDto createReleaseNoteMessage(Project project, ReleaseNote releaseNote) {
        return ReleaseNoteMessageDto.builder()
                .type("Release Note")
                .projectId(project.getProjectId())
                .projectName(project.getTitle())
                .projectImg(project.getImg())
                .message("릴리즈 노트의 배포 동의 여부를 선택해 주세요.")
                .date(Date.from(releaseNote.getCreatedDate().atZone(ZoneId.systemDefault()).toInstant()))
                .releaseNoteId(releaseNote.getReleaseId())
                .build();
    }

    /**
     * 프로젝트를 이용하여 프로젝트 멤버 이메일 목록을 가져온다.
     *
     * @param project 프로젝트
     * @return 프로젝트 멤버 이메일 목록
     * @author seonwoo
     * @date 2023-08-14 (월)
     */
    private List<String> findProjectMembersByProject(Project project) {
        return projectMemberRepository.findByProject(project).stream()
                .map(m -> m.getUser().getEmail())
                .collect(Collectors.toList());
    }

    /**
     * 알림 식별 문자를 이용하여 알림 데이터를 가져온다.
     *
     * @param notificationId 알림 식별 문자
     * @return Notification
     * @author seonwoo
     * @date 2023-08-15 (화)
     */
    private Notification findNotificationById(String notificationId) {
        return notificationRedisRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(NOT_EXISTS_NOTIFICATION));
    }

    /**
     * 해당 사용자의 알림 읽음 여부 데이터를 업데이트한다.
     *
     * @param email        사용자 이메일
     * @param notification 알림 정보
     * @author seonwoo
     * @date 2023-08-15 (화)
     */
    private void updateIsReadByUserEmail(String email, Notification notification) {
        // markByUsers에서 key 값이 현재 email 값에 해당하는 읽음 여부 값을 업데이트한다. (1: 읽음, 0: 안 읽음)
        notification.updateMarkByUsers(email, 1);

        // 업데이트된 값을 저장한다.
        notificationRedisRepository.save(notification);
    }
}

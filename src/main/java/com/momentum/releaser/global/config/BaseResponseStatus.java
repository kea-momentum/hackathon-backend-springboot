package com.momentum.releaser.global.config;

import com.momentum.releaser.domain.project.dto.ProjectMemberResponseDto.InviteProjectMemberResponseDTO;
import lombok.Getter;

/**
 * 에러 코드 관리
 */
@Getter
public enum BaseResponseStatus {
    /**
     * 1000 : 요청 성공
     */
    SUCCESS(true, 1000, "요청에 성공하였습니다."),
    SUCCESS_TO_UPDATE_RELEASE_NOTE(true, 1400, "릴리즈 노트 수정에 성공하였습니다."),

    /**
     * 2000 : Request 오류
     */

    INVALID_REQUEST_BODY(false, 2000, "요청 데이터가 잘못되었습니다."),
    INVALID_QUERY_STRING(false, 2001, "QueryString 요청 데이터가 잘못되었습니다."),
    NOT_EXISTS_REDIS_CODE(false, 2002, "유효하지 않은 코드입니다."),
    INVALID_REDIS_CODE(false, 2003, "잘못된 코드입니다."),
    INVALID_REDIS_KEY(false, 2004, "유효하지 않은 키 값입니다."),
    INVALID_FILTER_TYPE(false, 2005, "유효하지않은 필터 타입입니다."),

    NOT_EQUAL_PASSWORD_AND_CONFIRM_PASSWORD(false, 2100, "비밀번호와 확인용 비밀번호가 일치하지 않습니다."),
    NOT_PROJECT_PM(false, 2200, "해당 프로젝트의 관리자가 아닙니다."),

    INVALID_RELEASE_VERSION_TYPE(false, 2400, "릴리즈 버전 타입이 올바르지 않습니다. MAJOR, MINOR, PATCH 중 하나여야 합니다."),
    EXISTS_DEPLOYED_RELEASE_NOTE_AFTER_THIS(false, 2401, "배포된 상위 버전의 릴리즈 노트가 있어 삭제할 수 없습니다."),
    EXISTS_NOT_DEPLOYED_RELEASE_NOTE_BEFORE_THIS(false, 2402, "배포되지 않은 하위 버전의 릴리즈 노트가 있어 현재 릴리즈 노트를 배포할 수 없습니다."),
    UNAUTHORIZED_TO_DELETE_RELEASE_OPINION(false, 2403, "해당 릴리즈 노트 의견 삭제 권한이 없습니다."),
    ALREADY_DEPLOYED_RELEASE_NOTE(false, 2404, "이미 배포된 릴리즈 노트입니다."),
    ALREADY_ALL_APPROVALS_WITH_YES(false, 2405, "이미 모든 멤버의 동의가 완료되었습니다."),

    INVALID_ISSUE_TAG(false, 2500, "이슈 태그가 올바르지 않습니다."),
    INVALID_LIFECYCLE(false, 2501, "이슈 상태가 올바르지 않습니다."),
    CONNECTED_ISSUE_EXISTS(false, 2502, "릴리즈와 연결된 이슈이므로 상태 변경이 불가능합니다."),
    CONNECTED_RELEASE_EXISTS(false, 2503, "연결된 릴리즈가 존재하므로 삭제할 수 없습니다."),
    NOT_ADMIN(false, 2504, "프로젝트의 관리자만 수정이 가능합니다."),

    NOT_EXISTS_NOTIFICATION(false, 2700, "존재하지 않는 알림 내역입니다."),
    NOT_EXISTS_NOTIFICATION_PER_USER(false, 2701, "존재하지 않는 사용자 알림 데이터입니다."),


    /**
     * 3000 : Response 오류
     */


    /**
     * 4000 : Database, Server 오류
     */
    DATABASE_ERROR(false, 4000, "데이터베이스 연결에 실패하였습니다."),
    SERVER_ERROR(false, 4001, "서버와의 연결에 실패하였습니다."),
    IO_ERROR(false, 4002, "입출력 예외가 발생하였습니다."),
    NOT_EXISTS_S3_FILE(false, 4003, "존재하지 않는 파일입니다."),

    NOT_EXISTS_USER(false, 4100, "존재하지 않는 유저입니다."),
    FAILED_TO_UPDATE_USER_PROFILE_IMG(false, 4101, "사용자 프로필 이미지 변경에 실패하였습니다."),
    OVERLAP_CHECK_EMAIL(false, 4102, "중복된 이메일입니다."),
    NOT_MATCHES_PASSWORD(false, 4103, "일치하는 비밀번호가 없습니다."),
    INVALID_REFRESH_TOKEN(false, 4104, "유효하지 않은 Refresh Token 입니다."),
    INVALID_USER_NAME(false, 4105, "사용자 정보가 올바르지 않습니다."),
    PROJECT_DELETION_REQUIRED_FOR_USER_WITHDRAWAL(false, 4106, "관리자인 프로젝트가 존재하므로 탈퇴할 수 없습니다."),

    NOT_EXISTS_PROJECT(false, 4200, "존재하지 않는 프로젝트입니다."),
    FAILED_TO_CREATE_PROJECT(false, 4201, "프로젝트 생성에 실패하였습니다."),
    NOT_EXISTS_LINK(false, 4202, "존재하지 않는 초대링크입니다."),

    NOT_EXISTS_PROJECT_MEMBER(false, 4300, "존재하지 않는 멤버입니다."),
    NOT_EXISTS_ADMIN_MEMBER(false, 4301, "관리자가 존재하지 않습니다."),
    ALREADY_EXISTS_PROJECT_MEMBER(false, 4302, "이미 존재하는 멤버입니다."),
    NOT_PROJECT_MANAGER(false, 4303, "프로젝트 관리자가 아닙니다."),

    NOT_EXISTS_RELEASE_NOTE(false, 4400, "존재하지 않는 릴리즈 노트입니다."),
    FAILED_TO_CREATE_RELEASE_NOTE(false, 4401, "릴리즈 노트 생성에 실패하였습니다."),
    FAILED_TO_GET_LATEST_RELEASE_VERSION(false, 4402, "릴리즈 노트 버전 불러오기에 실패하였습니다."),
    FAILED_TO_UPDATE_RELEASE_NOTE(false, 4403, "릴리즈 노트 수정에 실패하였습니다."),
    DUPLICATED_RELEASE_VERSION(false, 4404, "이미 존재하는 릴리즈 버전입니다."),
    INVALID_RELEASE_VERSION(false, 4405, "올바르지 않은 릴리즈 버전입니다."),
    FAILED_TO_UPDATE_INITIAL_RELEASE_VERSION(false, 4406, "릴리즈 노트 1.0.0은 버전을 수정할 수 없습니다."),
    FAILED_TO_UPDATE_DEPLOYED_RELEASE_VERSION(false, 4407, "이미 배포된 릴리즈 노트는 수정할 수 없습니다."),
    FAILED_TO_UPDATE_RELEASE_DEPLOY_STATUS(false, 4408, "해당 릴리즈 노트의 배포 상태는 수정할 수 없습니다."),
    FAILED_TO_DELETE_DEPLOYED_RELEASE_NOTE(false, 4409, "이미 배포된 릴리즈 노트는 삭제할 수 없습니다."),
    FAILED_TO_DELETE_RELEASE_NOTE(false, 4410, "릴리즈 노트 삭제에 실패하였습니다."),
    FAILED_TO_APPROVE_RELEASE_NOTE(false, 4411, "릴리즈 노트 배포 동의를 체크할 수 없습니다."),
    UNAUTHORIZED_RELEASE_NOTE(false, 4412, "해당 프로젝트 멤버는 해당 릴리즈 노트에 접근 권한이 없습니다."),
    NOT_EXISTS_RELEASE_APPROVAL(false, 4413, "존재하지 않는 릴리즈 배포 동의 여부 데이터입니다."),
    FAILED_TO_GET_RELEASE_APPROVALS(false, 4414, "릴리즈 노트 배포 동의 데이터를 불러오기에 실패하였습니다."),
    NOT_EXISTS_RELEASE_OPINION(false, 4415, "존재하지 않는 릴리즈 노트 의견입니다."),
    EXISTS_DISAPPROVED_MEMBER(false, 4416, "릴리즈 노트 배포를 동의하지 않은 멤버가 있습니다."),

    NOT_EXISTS_ISSUE(false, 4500, "존재하지 않는 이슈입니다."),
    INVALID_ISSUE_WITH_COMPLETED(false, 4501, "이미 연결된 이슈가 포함되어 있습니다."),
    INVALID_ISSUE_WITH_NOT_DONE(false, 4502, "완료되지 않은 이슈는 연결할 수 없습니다."),
    FAILED_TO_CONNECT_ISSUE_WITH_RELEASE_NOTE(false, 4503, "이슈 연결에 실패하였습니다."),
    NOT_EXISTS_ISSUE_OPINION(false, 4504, "존재하지 않는 이슈 의견입니다."),
    NOT_ISSUE_COMMENTER(false, 4505, "해당 의견 작성자가 아닙니다."),
    INVALID_ISSUE(false, 4506, "유효한 이슈가 아닙니다."),

    NOT_EXISTS_USERS_IN_NOTIFICATION_DATA(false, 4700, "사용자 정보가 알림 데이터 안에 존재하지 않습니다.");

    private final boolean isSuccess;
    private final int code;
    private final String message;

    private BaseResponseStatus(boolean isSuccess, int code, String message) { //BaseResponseStatus 에서 각 해당하는 코드를 생성자로 맵핑
        this.isSuccess = isSuccess;
        this.code = code;
        this.message = message;
    }

    public String getErrorMessage(Long releaseId) {

        return message + " (releaseId: " + releaseId + ")";
    }

    public String getErrorMessageDto(InviteProjectMemberResponseDTO inviteProjectMemberRes) {

        return message + inviteProjectMemberRes;
    }



}

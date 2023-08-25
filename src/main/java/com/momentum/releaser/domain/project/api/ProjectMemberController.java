package com.momentum.releaser.domain.project.api;

import javax.validation.constraints.Min;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

import com.momentum.releaser.domain.project.application.ProjectMemberService;
import com.momentum.releaser.domain.project.dto.ProjectMemberResponseDto.InviteProjectMemberResponseDTO;
import com.momentum.releaser.domain.project.dto.ProjectMemberResponseDto.MembersResponseDTO;
import com.momentum.releaser.global.config.BaseResponse;
import com.momentum.releaser.global.jwt.UserPrincipal;


/**
 * ProjectMemberController는 프로젝트 멤버와 관련된 API 엔드포인트를 처리하는 컨트롤러입니다.
 * 조회, 추가, 삭제 기능을 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Validated
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    /**
     * 4.1 프로젝트 멤버 조회
     *
     * @param projectId 프로젝트 식별 번호
     * @param userPrincipal 인증된 사용자의 정보
     * @return MembersResponseDTO 프로젝트 멤버 목록
     */
    @GetMapping("/project/{projectId}")
    public BaseResponse<MembersResponseDTO> projectMemberList(
            @PathVariable @Min(value = 1, message = "프로젝트 식별 번호는 1 이상의 숫자여야 합니다.") Long projectId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        String email = userPrincipal.getEmail();
        return new BaseResponse<>(projectMemberService.findProjectMembers(projectId, email));
    }

    /**
     * 4.2 프로젝트 멤버 추가
     *
     * @param link 프로젝트 가입 링크
     * @param userPrincipal 인증된 사용자의 정보
     * @return InviteProjectMemberResponseDTO, String 초대된 멤버 정보와 메시지
     */
    @PostMapping("/join/{link}")
    public BaseResponse<InviteProjectMemberResponseDTO> memberAdd(
            @PathVariable String link,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        String email = userPrincipal.getEmail();
        String message = "프로젝트 참여가 완료되었습니다.";
        return new BaseResponse<>(projectMemberService.addProjectMember(link, email), message);
    }

    /**
     * 4.3 프로젝트 멤버 제거
     *
     * @param memberId 프로젝트 멤버 식별 번호
     * @param userPrincipal 인증된 사용자의 정보
     * @return String "프로젝트 멤버가 제거되었습니다."
     */
    @PostMapping("/{memberId}")
    public BaseResponse<String> ProjectMemberRemove(
            @PathVariable @Min(value = 1, message = "프로젝트 멤버 식별 번호는 1 이상의 숫자여야 합니다.") Long memberId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        String email = userPrincipal.getEmail();
        return new BaseResponse<>(projectMemberService.removeProjectMember(memberId, email));
    }

    /**
     * 4.4 프로젝트 멤버 탈퇴
     *
     * @param projectId  프로젝트 식별 번호
     * @param userPrincipal  인증된 사용자의 정보
     * @return String "프로젝트 탈퇴가 완료되었습니다."
     */
    @PostMapping("/project/{projectId}/withdraw")
    public BaseResponse<String> withdrawProjectMemberRemove(
            @PathVariable @Min(value = 1, message = "프로젝트 식별 번호는 1 이상의 숫자여야 합니다.") Long projectId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        String email = userPrincipal.getEmail();
        return new BaseResponse<>(projectMemberService.removeWithdrawProjectMember(projectId, email));
    }
}

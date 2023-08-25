package com.momentum.releaser.domain.issue.api;

import static com.momentum.releaser.domain.issue.dto.IssueRequestDto.*;
import static com.momentum.releaser.domain.issue.dto.IssueResponseDto.*;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

import com.momentum.releaser.domain.issue.application.IssueService;
import com.momentum.releaser.domain.issue.dto.IssueRequestDto.IssueInfoRequestDTO;
import com.momentum.releaser.domain.issue.dto.IssueResponseDto.ConnectionIssuesResponseDTO;
import com.momentum.releaser.domain.issue.dto.IssueResponseDto.DoneIssuesResponseDTO;
import com.momentum.releaser.domain.issue.dto.IssueResponseDto.OpinionInfoResponseDTO;
import com.momentum.releaser.global.config.BaseResponse;
import com.momentum.releaser.global.jwt.UserPrincipal;

/**
 * IssueController는 이슈와 관련된 API 엔드포인트를 처리하는 컨트롤러입니다.
 * 생성, 수정, 삭제, 조회 기능을 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/issues")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Validated
public class IssueController {

    private final IssueService issueService;

    /**
     * 7.1 이슈 생성
     *
     * @param projectId   프로젝트 식별 번호
     * @param registerReq 이슈 정보 등록 요청 DTO
     * @return IssueIdResponseDTO 이슈 생성 결과를 담은 응답 DTO
     */
    @PostMapping("/{projectId}")
    public BaseResponse<IssueIdResponseDTO> issueAdd(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                     @PathVariable @Min(value = 1, message = "프로젝트 식별 번호는 1 이상의 숫자여야 합니다.") Long projectId,
                                                     @Valid @RequestBody IssueInfoRequestDTO registerReq) {
        return new BaseResponse<>(issueService.addIssue(userPrincipal.getEmail(), projectId, registerReq));
    }

    /**
     * 7.2 이슈 수정
     *
     * @param issueId       이슈 식별 번호
     * @param userPrincipal 인증된 사용자 정보를 담고 있는 객체
     * @param updateReq     이슈 수정 요청 DTO
     * @return String "이슈 수정이 완료되었습니다."
     */
    @PatchMapping("/issue/{issueId}")
    public BaseResponse<IssueModifyResponseDTO> issueModify(@PathVariable @Min(value = 1, message = "이슈 식별 번호는 1 이상의 숫자여야 합니다.") Long issueId,
                                                            @AuthenticationPrincipal UserPrincipal userPrincipal,
                                                            @Valid @RequestBody IssueInfoRequestDTO updateReq) {
        String email = userPrincipal.getEmail();
        String message = "이슈 수정이 완료되었습니다.";
        return new BaseResponse<>(issueService.modifyIssue(issueId, email, updateReq), message);
    }

    /**
     * 7.3 이슈 제거
     *
     * @param issueId 이슈 식별 번호
     * @return String "이슈가 삭제되었습니다."
     */
    @PostMapping("/{issueId}/delete")
    public BaseResponse<String> issueRemove(@PathVariable @Min(value = 1, message = "이슈 식별 번호는 1 이상의 숫자여야 합니다.") Long issueId) {
        return new BaseResponse<>(issueService.removeIssue(issueId));
    }

    /**
     * 7.4 프로젝트별 모든 이슈 조회
     *
     * @param projectId 프로젝트 식별 번호
     * @return AllIssueListResponseDTO 이슈 조회 결과를 담은 응답 DTO
     */
    @GetMapping("/project/{projectId}")
    public BaseResponse<AllIssueListResponseDTO> allIssueList(@PathVariable @Min(value = 1, message = "프로젝트 식별 번호는 1 이상의 숫자여야 합니다.") Long projectId) {
        return new BaseResponse<>(issueService.findAllIssues(projectId));
    }

    /**
     * 7.5 프로젝트별 해결 & 미연결 이슈 조회
     *
     * @param projectId 프로젝트 식별 번호
     * @param status    이슈 상태 (DONE 이어야 함)
     * @param connect   연결 상태 (false 이어야 함)
     * @return DoneIssuesResponseDTO 이슈 조회 결과를 담은 응답 DTO
     */
    @GetMapping("/project/{projectId}/release")
    public BaseResponse<List<DoneIssuesResponseDTO>> doneIssueList(@PathVariable @Min(value = 1, message = "프로젝트 식별 번호는 1 이상의 숫자여야 합니다.") Long projectId,
                                                                   @RequestParam
                                                                   @Pattern(regexp = "(?i)^(DONE)$", message = "상태는 DONE 이어야 합니다.") String status,
                                                                   @RequestParam
                                                                   @Pattern(regexp = "(?i)^(false)$", message = "연결 상태는 false 이어야 합니다.") String connect) {

        return new BaseResponse<>(issueService.findDoneIssues(projectId, status));
    }

    /**
     * 7.6 릴리즈 노트별 연결된 이슈 조회
     *
     * @param projectId 프로젝트 식별 번호
     * @param releaseId 릴리즈 노트 식별 번호
     * @param connect   연결 상태 (true 이어야 함)
     * @return ConnectionIssuesResponseDTO 이슈 조회 결과를 담은 응답 DTO
     */
    @GetMapping("/project/{projectId}/release/{releaseId}")
    public BaseResponse<List<ConnectionIssuesResponseDTO>> connectIssueList(@PathVariable @Min(value = 1, message = "프로젝트 식별 번호는 1 이상의 숫자여야 합니다.") Long projectId,
                                                                            @PathVariable @Min(value = 1, message = "릴리즈 노트 식별 번호는 1 이상의 숫자여야 합니다.") Long releaseId,
                                                                            @RequestParam(required = false, defaultValue = "true") boolean connect) {
        return new BaseResponse<>(issueService.findConnectIssues(projectId, releaseId));
    }

    /**
     * 7.7 이슈별 조회
     *
     * @param issueId       이슈 식별 번호
     * @param userPrincipal 인증된 사용자 정보를 담고 있는 객체
     * @return IssueDetailsDTO 이슈별 조회 결과를 담은 응답 DTO
     */
    @GetMapping("/{issueId}")
    public BaseResponse<IssueDetailsDTO> issueDetails(@PathVariable @Min(value = 1, message = "이슈 식별 번호는 1 이상의 숫자여야 합니다.") Long issueId,
                                                      @AuthenticationPrincipal UserPrincipal userPrincipal) {
        String email = userPrincipal.getEmail();
        return new BaseResponse<>(issueService.findIssue(issueId, email));
    }

    /**
     * 7.8 이슈 상태 변경
     *
     * @param issueId   이슈 식별 번호
     * @param lifeCycle 변경할 이슈 상태 (NOT_STARTED, IN_PROGRESS, DONE 중 하나)
     * @return String "이슈 상태 변경이 완료되었습니다."
     */
    @PatchMapping("/{issueId}")
    public BaseResponse<String> issueLifeCycleModify(@PathVariable @Min(value = 1, message = "이슈 식별 번호는 1 이상의 숫자여야 합니다.") Long issueId,
                                                     @RequestParam(name = "index") Integer index,
                                                     @RequestParam(name = "status")
                                                     @Pattern(regexp = "(?i)^(NOT_STARTED|IN_PROGRESS|DONE)$", message = "상태는 NOT_STARTED, IN_PROGRESS, DONE 중 하나여야 합니다.")
                                                     String lifeCycle) {
        return new BaseResponse<>(issueService.modifyIssueLifeCycle(issueId, index, lifeCycle));
    }

    /**
     * 8.1 이슈 의견 추가
     *
     * @param issueId       이슈의 식별 번호
     * @param userPrincipal 인증된 사용자 정보를 담고 있는 객체
     * @param opinionReq    등록할 의견 정보
     * @return OpinionInfoResponseDTO 의견 등록 후의 이슈에 대한 모든 의견 정보 DTO
     */
    @PostMapping("/{issueId}/opinion")
    public BaseResponse<List<OpinionInfoResponseDTO>> issueOpinionAdd(@PathVariable @Min(value = 1, message = "이슈 식별 번호는 1 이상의 숫자여야 합니다.") Long issueId,
                                                                      @AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                      @Valid @RequestBody RegisterOpinionRequestDTO opinionReq) {
        String email = userPrincipal.getEmail();
        return new BaseResponse<>(issueService.addIssueOpinion(issueId, email, opinionReq));
    }

    /**
     * 8.2 이슈 의견 삭제
     *
     * @param opinionId     이슈 의견의 식별 번호
     * @param userPrincipal 인증된 사용자 정보를 담고 있는 객체
     * @return OpinionInfoResponseDTO 의견 삭제 후의 이슈에 대한 모든 의견 정보 DTO
     */
    @PostMapping("/opinion/{opinionId}")
    public BaseResponse<List<OpinionInfoResponseDTO>> issueOpinionRemove(@PathVariable @Min(value = 1, message = "이슈 의견 식별 번호는 1 이상의 숫자여야 합니다.") Long opinionId,
                                                                         @AuthenticationPrincipal UserPrincipal userPrincipal) {
        String email = userPrincipal.getEmail();
        return new BaseResponse<>(issueService.removeIssueOpinion(opinionId, email));
    }

}

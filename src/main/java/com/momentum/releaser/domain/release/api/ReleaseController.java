package com.momentum.releaser.domain.release.api;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Min;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.momentum.releaser.domain.release.application.ReleaseService;
import com.momentum.releaser.domain.release.dto.ReleaseRequestDto.*;
import com.momentum.releaser.domain.release.dto.ReleaseResponseDto.*;
import com.momentum.releaser.global.config.BaseResponse;
import com.momentum.releaser.global.jwt.UserPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ReleaseController는 이슈와 관련된 API 엔드포인트를 처리하는 컨트롤러입니다.
 * 생성, 수정, 삭제, 조회 기능을 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/releases")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Validated
public class ReleaseController {

    private final ReleaseService releaseService;

    /**
     * 5.1 프로젝트별 릴리즈 노트 목록 조회
     *
     * @param userPrincipal 인증된 사용자 정보를 담고 있는 객체
     * @param projectId     프로젝트 식별 번호
     * @return ReleasesResponseDTO 릴리즈 정보 리스트를 담은 응답 DTO
     */
    @GetMapping(value = "/projects")
    public BaseResponse<ReleasesResponseDTO> releaseNoteList(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                             @RequestParam @Min(value = 1, message = "프로젝트 식별 번호는 1 이상의 숫자여야 합니다.") Long projectId) {

        return new BaseResponse<>(releaseService.findReleaseNotes(userPrincipal.getEmail(), projectId));
    }

    /**
     * 5.2 릴리즈 노트 생성
     *
     * @param userPrincipal           인증된 사용자 정보를 담고 있는 객체
     * @param projectId               프로젝트 식별 번호
     * @param releaseCreateRequestDto 릴리즈 생성 정보를 담은 요청 DTO
     * @return ReleaseCreateAndUpdateResponseDTO 릴리즈 생성 결과를 담은 응답 DTO
     */
    @PostMapping(value = "/projects/{projectId}")
    public BaseResponse<ReleaseCreateAndUpdateResponseDTO> releaseNoteAdd(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                          @PathVariable @Min(value = 1, message = "프로젝트 식별 번호는 1 이상의 숫자여야 합니다.") Long projectId,
                                                                          @RequestBody @Valid ReleaseCreateRequestDTO releaseCreateRequestDto) {

        return new BaseResponse<>(releaseService.addReleaseNote(userPrincipal.getEmail(), projectId, releaseCreateRequestDto));
    }

    /**
     * 5.3 릴리즈 노트 수정
     *
     * @param userPrincipal           인증된 사용자 정보를 담고 있는 객체
     * @param releaseId               릴리즈 노트 식별 번호
     * @param releaseUpdateRequestDto 릴리즈 수정 정보를 담은 요청 DTO
     * @return ReleaseCreateAndUpdateResponseDTO 릴리즈 수정 결과를 담은 응답 DTO
     */
    @PatchMapping(value = "/{releaseId}")
    public BaseResponse<ReleaseCreateAndUpdateResponseDTO> releaseNoteSave(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                           @PathVariable @Min(value = 1, message = "릴리즈 식별 번호는 1 이상의 숫자여야 합니다.") Long releaseId,
                                                                           @RequestBody @Valid ReleaseUpdateRequestDTO releaseUpdateRequestDto) {

        return new BaseResponse<>(releaseService.saveReleaseNote(userPrincipal.getEmail(), releaseId, releaseUpdateRequestDto));
    }

    /**
     * 5.4 릴리즈 노트 삭제
     *
     * @param userPrincipal 인증된 사용자 정보를 담고 있는 객체
     * @param releaseId     릴리즈 노트 식별 번호
     * @return String "릴리즈 노트 삭제에 성공하였습니다."
     */
    @PostMapping(value = "/{releaseId}")
    public BaseResponse<String> releaseNoteRemove(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                  @PathVariable @Min(value = 1, message = "릴리즈 식별 번호는 1 이상의 숫자여야 합니다.") Long releaseId) {

        return new BaseResponse<>(releaseService.removeReleaseNote(userPrincipal.getEmail(), releaseId));
    }

    /**
     * 5.5 릴리즈 노트 조회
     *
     * @param userPrincipal 인증된 사용자 정보를 담고 있는 객체
     * @param releaseId     릴리즈 노트 식별 번호
     * @return ReleaseInfoResponseDTO 조회된 릴리즈 노트 정보를 담은 응답 DTO
     */
    @GetMapping(value = "/{releaseId}")
    public BaseResponse<ReleaseInfoResponseDTO> releaseNoteDetails(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                   @PathVariable @Min(value = 1, message = "릴리즈 식별 번호는 1 이상의 숫자여야 합니다.") Long releaseId) {

        return new BaseResponse<>(releaseService.findReleaseNote(userPrincipal.getEmail(), releaseId));
    }

    /**
     * 5.6 릴리즈 노트 배포 동의 여부 선택
     *
     * @param userPrincipal             인증된 사용자 정보를 담고 있는 객체
     * @param releaseId                 릴리즈 노트 식별 번호
     * @param releaseApprovalRequestDto 배포 동의 여부를 담은 요청 DTO
     * @return ReleaseApprovalsResponseDTO 배포 동의 여부 결과를 담은 응답 DTO
     */
    @PostMapping(value = "/{releaseId}/approvals")
    public BaseResponse<List<ReleaseApprovalsResponseDTO>> releaseApprovalModify(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                                 @PathVariable @Min(value = 1, message = "릴리즈 식별 번호는 1 이상의 숫자여야 합니다.") Long releaseId,
                                                                                 @RequestBody @Valid ReleaseApprovalRequestDTO releaseApprovalRequestDto) {

        return new BaseResponse<>(releaseService.modifyReleaseApproval(userPrincipal.getEmail(), releaseId, releaseApprovalRequestDto));
    }

    /**
     * 5.7 릴리즈 노트 그래프 좌표 추가
     *
     * @param releaseNoteCoordinateRequestDto 그래프 좌표를 담은 요청 DTO
     * @return String "릴리즈 노트 좌표 업데이트에 성공하였습니다."
     */
    @PostMapping(value = "/coordinates")
    public BaseResponse<String> releaseCoordinateModify(@RequestBody @Valid ReleaseNoteCoordinateRequestDTO releaseNoteCoordinateRequestDto) {

        return new BaseResponse<>(releaseService.modifyReleaseCoordinate(releaseNoteCoordinateRequestDto));
    }

    /**
     * 6.1 릴리즈 노트 의견 추가
     *
     * @param userPrincipal 인증된 사용자 정보를 담고 있는 객체
     * @return ReleaseOpinionsResponseDTO 릴리즈 의견 정보를 담은 응답 DTO
     */
    @PostMapping(value = "/{releaseId}/opinions")
    public BaseResponse<List<ReleaseOpinionsResponseDTO>> releaseOpinionAdd(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                            @PathVariable @Min(value = 1, message = "릴리즈 식별 번호는 1 이상의 숫자여야 합니다.") Long releaseId,
                                                                            @RequestBody @Valid ReleaseOpinionCreateRequestDTO releaseOpinionCreateRequestDto) {

        return new BaseResponse<>(releaseService.addReleaseOpinion(userPrincipal.getEmail(), releaseId, releaseOpinionCreateRequestDto));
    }

    /**
     * 6.2 릴리즈 노트 의견 삭제
     *
     * @param userPrincipal 인증된 사용자 정보를 담고 있는 객체
     * @param opinionId     릴리즈 의견 식별 번호
     * @return ReleaseOpinionsResponseDTO 릴리즈 의견 정보를 담은 응답 DTO
     */
    @PostMapping("/opinions/{opinionId}")
    public BaseResponse<List<ReleaseOpinionsResponseDTO>> releaseOpinionRemove(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                               @PathVariable @Min(value = 1, message = "릴리즈 의견 식별 번호는 1 이상의 숫자여야 합니다.") Long opinionId) {

        return new BaseResponse<>(releaseService.removeReleaseOpinion(userPrincipal.getEmail(), opinionId));
    }

    /**
     * 6.3 릴리즈 노트 의견 목록 조회
     *
     * @param releaseId 릴리즈 식별 번호
     * @return ReleaseOpinionsResponseDTO 릴리즈 의견 정보를 담은 응답 DTO
     */
    @GetMapping("/{releaseId}/opinions")
    public BaseResponse<List<ReleaseOpinionsResponseDTO>> releaseOpinionList(@PathVariable @Min(value = 1, message = "릴리즈 식별 번호는 1 이상의 숫자여야 합니다.") Long releaseId) {

        return new BaseResponse<>(releaseService.findReleaseOpinions(releaseId));
    }

    /**
     * 9.1 프로젝트별 릴리즈 보고서 조회
     *
     * @param projectId 프로젝트 식별 번호
     * @return ReleaseDocsResponseDTO 릴리즈 문서 정보를 담은 응답 DTO
     */
    @GetMapping("/project/{projectId}/docs")
    public BaseResponse<List<ReleaseDocsResponseDTO>> releaseDocsList(@PathVariable @Min(value = 1, message = "프로젝트 식별 번호는 1 이상의 숫자여야 합니다.") Long projectId) {

        return new BaseResponse<>(releaseService.findReleaseDocs(projectId));
    }

    /**
     * 9.2 프로젝트별 릴리즈 보고서 수정
     *
     * @param projectId            프로젝트 식별 번호
     * @param userPrincipal        인증된 사용자 정보를 담고 있는 객체
     * @param updateReleaseDocsReq 릴리즈 문서의 수정 요청 DTO
     * @return String "릴리즈 보고서가 수정되었습니다."
     */
    @PatchMapping("/project/{projectId}/docs")
    public BaseResponse<String> releaseDocsModify(@PathVariable @Min(value = 1, message = "프로젝트 식별 번호는 1 이상의 숫자여야 합니다.") Long projectId,
                                                  @AuthenticationPrincipal UserPrincipal userPrincipal,
                                                  @RequestBody @Valid List<UpdateReleaseDocsRequestDTO> updateReleaseDocsReq) {
        String email = userPrincipal.getEmail();
        return new BaseResponse<>(releaseService.modifyReleaseDocs(projectId, email, updateReleaseDocsReq));
    }
}

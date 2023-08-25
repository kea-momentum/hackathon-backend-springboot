package com.momentum.releaser.domain.user.api;

import java.io.IOException;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.momentum.releaser.domain.user.application.UserService;
import com.momentum.releaser.global.config.BaseResponse;
import com.momentum.releaser.global.jwt.UserPrincipal;
import com.momentum.releaser.domain.user.dto.UserRequestDto.UserUpdateImgRequestDTO;
import com.momentum.releaser.domain.user.dto.UserResponseDto.UserProfileImgResponseDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * UserController는 사용자 인증과 관련된 API 엔드포인트를 처리하는 컨트롤러입니다.
 * 사용자 관리(조회, 수정, 삭제) 기능을 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Validated
public class UserController {

    private final UserService userService;

    /**
     * 1.1 사용자 프로필 이미지 조회
     *
     * @param userPrincipal 인증된 사용자 정보
     * @return UserProfileImgResponseDTO 사용자 프로필 이미지 정보
     */
    @GetMapping(value = "/images")
    public BaseResponse<UserProfileImgResponseDTO> userProfileImgDetails(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return new BaseResponse<>(userService.findUserProfileImg(userPrincipal.getEmail()));
    }

    /**
     * 1.2 사용자 프로필 이미지 변경
     *
     * @param userPrincipal JWT
     * @param userUpdateImgRequestDto 사용자 프로필 이미지 변경 요청 정보
     * @return UserProfileImgResponseDTO 사용자 프로필 이미지 정보
     * @throws IOException 파일 입출력 관련 예외
     */
    @PatchMapping(value = "/images")
    public BaseResponse<UserProfileImgResponseDTO> userProfileImgModify(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                     @RequestBody UserUpdateImgRequestDTO userUpdateImgRequestDto) throws IOException {
        return new BaseResponse<>(userService.modifyUserProfileImg(userPrincipal.getEmail(), userUpdateImgRequestDto));
    }

    /**
     * 1.3 사용자 프로필 이미지 삭제
     *
     * @param userPrincipal JWT
     * @return UserProfileImgResponseDTO 사용자 프로필 이미지 정보
     */
    @PostMapping(value = "/images")
    public BaseResponse<UserProfileImgResponseDTO> userProfileImgRemove(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return new BaseResponse<>(userService.removeUserProfileImg(userPrincipal.getEmail()));
    }

    /**
     * 1.4 사용자 탈퇴
     * @param userPrincipal JWT
     * @return String "탈퇴가 완료되었습니다."
     */
    @PostMapping(value = "/withdraw")
    public BaseResponse<String> userRemove(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        String email = userPrincipal.getEmail();
        return new BaseResponse<>(userService.removeUser(email));
    }
}

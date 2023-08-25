package com.momentum.releaser.domain.user.application;

import java.io.IOException;

import com.momentum.releaser.domain.user.dto.UserRequestDto.UserUpdateImgRequestDTO;
import com.momentum.releaser.domain.user.dto.UserResponseDto.UserProfileImgResponseDTO;

/**
 * 사용자 관리와 관련된 기능을 제공하는 인터페이스입니다.
 */
public interface UserService {

    /**
     * 1.1 사용자 프로필 이미지 조회
     */
    UserProfileImgResponseDTO findUserProfileImg(String userEmail);

    /**
     * 1.2 사용자 프로필 이미지 변경
     */
    UserProfileImgResponseDTO modifyUserProfileImg(String userEmail, UserUpdateImgRequestDTO userUpdateImgRequestDto) throws IOException;

    /**
     * 1.3 사용자 프로필 이미지 삭제
     */
    UserProfileImgResponseDTO removeUserProfileImg(String userEmail);

    /**
     * 1.4 사용자 탈퇴
     */
    String removeUser(String userEmail);
}

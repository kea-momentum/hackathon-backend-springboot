package com.momentum.releaser.domain.user.application;

import com.momentum.releaser.domain.user.dto.AuthRequestDto;
import com.momentum.releaser.domain.user.dto.AuthRequestDto.ConfirmAuthCodeRequestDTO;
import com.momentum.releaser.domain.user.dto.AuthRequestDto.SavePasswordRequestDTO;
import com.momentum.releaser.domain.user.dto.AuthRequestDto.UserInfoReqestDTO;
import com.momentum.releaser.domain.user.dto.AuthRequestDto.UserLoginReqestDTO;
import com.momentum.releaser.domain.user.dto.AuthResponseDto;
import com.momentum.releaser.domain.user.dto.AuthResponseDto.ConfirmEmailResponseDTO;
import com.momentum.releaser.domain.user.dto.AuthResponseDto.ConfirmPasswordCodeResponseDTO;
import com.momentum.releaser.domain.user.dto.AuthResponseDto.UserInfoResponseDTO;
import com.momentum.releaser.domain.user.dto.TokenDto;

/**
 * 사용자 인증과 관련된 기능을 제공하는 인터페이스입니다.
 */
public interface AuthService {

    /**
     * 2.1 회원가입
     */
    UserInfoResponseDTO addSignUpUser(UserInfoReqestDTO userInfoReq);


    /**
     * 2.2 이메일 로그인
     */
    TokenDto saveLoginUser(UserLoginReqestDTO userLoginReq);

    /**
     * 2.3 Token 재발급
     */
    TokenDto saveRefreshUser(String accessToken, String refreshToken);

    /**
     * 2.7 이메일 인증 확인
     *
     * @author seonwoo
     * @date 2023-08-01 (화)
     * @param userEmail 사용자 이메일
     * @param confirmAuthCodeRequestDTO 사용자 이메일 인증 확인 코드
     * @return ConfirmEmailResponseDTO 사용자 이메일
     */
    ConfirmEmailResponseDTO confirmEmail(String userEmail, ConfirmAuthCodeRequestDTO confirmAuthCodeRequestDTO);

    /**
     * 2.8 비밀번호 변경 인증 확인
     *
     * @author seonwoo
     * @date 2023-08-02 (수)
     * @param email 사용자 이메일
     * @param name 사용자 이름
     * @param confirmAuthCodeRequestDTO 사용자 비밀번호 변경 인증 코드
     * @return ConfirmPasswordCodeResponseDTO 사용자 이메일, 사용자 이름
     */
    ConfirmPasswordCodeResponseDTO confirmPasswordCode(String email, String name, ConfirmAuthCodeRequestDTO confirmAuthCodeRequestDTO);

    /**
     * 2.10 비밀번호 변경
     *
     * @author seonwoo
     * @date 2023-08-02 (수)
     * @param email 사용자 이메일
     * @param savePasswordRequestDTO 변경하려는 비밀번호
     * @return 비밀번호 변경 성공 메시지
     */
    String savePassword(String email, SavePasswordRequestDTO savePasswordRequestDTO);
}

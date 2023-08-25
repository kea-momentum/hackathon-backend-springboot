package com.momentum.releaser.domain.user.api;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.nimbusds.oauth2.sdk.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.momentum.releaser.domain.user.dto.AuthResponseDto.ConfirmPasswordCodeResponseDTO;
import com.momentum.releaser.domain.user.dto.AuthRequestDto.*;
import com.momentum.releaser.domain.user.dto.AuthResponseDto.ConfirmEmailResponseDTO;
import com.momentum.releaser.domain.user.application.EmailService;
import com.momentum.releaser.domain.user.dto.AuthResponseDto.UserInfoResponseDTO;
import com.momentum.releaser.domain.user.application.AuthService;
import com.momentum.releaser.domain.user.dto.TokenDto;
import com.momentum.releaser.global.config.BaseResponse;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

/**
 * AuthController는 사용자 인증과 관련된 API 엔드포인트를 처리하는 컨트롤러입니다.
 * 회원가입, 로그인, Token 재발급 등의 기능을 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Validated
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;

    /**
     * 2.1 회원가입
     *
     * @param userInfoReq 회원가입 요청 객체
     * @return UserInfoResponseDTO 회원가입 성공 시 사용자 정보를 담은 DTO
     */
    @PostMapping("/signup")
    public BaseResponse<UserInfoResponseDTO> signUpUserAdd(
            @RequestBody @NotNull(message = "정보를 입력해주세요.") UserInfoReqestDTO userInfoReq) {
        return new BaseResponse<>(authService.addSignUpUser(userInfoReq));
    }

    /**
     * 2.2 이메일 로그인
     *
     * @param userLoginReq 로그인 요청 객체
     * @return TokenDto 로그인 성공 시 토큰 정보를 담은 DTO
     */
    @PostMapping("/login")
    public BaseResponse<TokenDto> loginUserSave(
            @RequestBody @NotNull(message = "정보를 입력해주세요.") UserLoginReqestDTO userLoginReq) {
        return new BaseResponse<>(authService.saveLoginUser(userLoginReq));
    }

    /**
     * 2.3 Token 재발급
     *
     * @param request HTTP 요청 객체
     * @return TokenDto Token 재발급 성공 시 새로운 Access Token 정보를 담은 DTO
     */
    @PostMapping("/refresh")
    public BaseResponse<TokenDto> refreshUserSave(HttpServletRequest request) {
        String accessToken = extractTokenFromAuthorizationHeader(request.getHeader("Access_Token"));
        String refreshToken = extractTokenFromAuthorizationHeader(request.getHeader("Refresh_Token"));
        return new BaseResponse<>(authService.saveRefreshUser(accessToken, refreshToken));
    }

    /**
     * 2.3 Token 재발급
     * Authorization 헤더에서 토큰을 추출
     *
     * @param authorizationHeader Authorization 헤더 값
     * @return 추출된 토큰
     */
    private String extractTokenFromAuthorizationHeader(String authorizationHeader) {
        return authorizationHeader.replace("Bearer ", "");
    }

    /**
     * 2.6 이메일 인증
     *
     * @param confirmEmailRequestDTO 인증이 필요한 이메일이 담긴 클래스
     * @return 이메일 인증 코드 메일 전송 성공 메시지
     * @throws MessagingException 이메일 전송 및 작성에 문제가 생긴 경우
     */
    @RequestMapping(value = "/emails", method = RequestMethod.POST, params = "!email")
    public BaseResponse<String> userEmailSend(
            @RequestBody @Valid SendEmailRequestDTO confirmEmailRequestDTO) throws MessagingException {

        return new BaseResponse<>(emailService.sendEmail(confirmEmailRequestDTO));
    }

    /**
     * 2.7 이메일 인증 확인
     *
     * @param email                     사용자 이메일
     * @param confirmAuthCodeRequestDTO 사용자 이메일 인증 확인 코드
     * @return ConfirmEmailRequestDTO 사용자 이메일
     * @author seonwoo
     * @date 2023-08-01 (화)
     */
    @RequestMapping(value = "/emails", method = RequestMethod.POST, params = "email")
    public BaseResponse<ConfirmEmailResponseDTO> userEmailConfirm(
            @RequestParam(value = "email")
            @NotBlank(message = "이메일을 입력해 주세요.") @Email(message = "올바르지 않은 이메일 형식입니다.") String email,
            @Valid @RequestBody ConfirmAuthCodeRequestDTO confirmAuthCodeRequestDTO) {

        return new BaseResponse<>(authService.confirmEmail(email, confirmAuthCodeRequestDTO));
    }

    /**
     * 2.8 비밀번호 변경 인증 메일 전송
     *
     * @param sendPasswordRequestDTO 사용자 이름, 이메일이 담긴 객체
     * @return 비밀번호 변경 인증 메일 전송 성공 메시지
     * @throws MessagingException 이메일 전송 및 작성에 문제가 생긴 경우
     * @author seonwoo
     * @date 2023-08-01 (화)
     */
    @RequestMapping(value = "/password", method = RequestMethod.POST, params = {"!email", "!name"})
    public BaseResponse<String> userPasswordSend(
            @RequestBody @Valid SendEmailForPasswordRequestDTO sendPasswordRequestDTO) throws MessagingException {

        return new BaseResponse<>(emailService.sendEmailForPassword(sendPasswordRequestDTO));
    }

    /**
     * 2.9 비밀번호 변경 인증 확인
     *
     * @param email                     사용자 이메일
     * @param name                      사용자 이름
     * @param confirmAuthCodeRequestDTO 비밀번호 변경 인증 코드가 담긴 객체
     * @return ConfirmPasswordCodeResponseDTO 사용자 이메일과 이름이 담긴 객체
     * @author seonwoo
     * @date 2023-08-02 (수)
     */
    @RequestMapping(value = "/password", method = RequestMethod.POST, params = {"email", "name"})
    public BaseResponse<ConfirmPasswordCodeResponseDTO> userPasswordCodeConfirm(
            @NotBlank(message = "이메일을 입력해 주세요.") @Email(message = "올바르지 않은 이메일 형식입니다.") @RequestParam(value = "email") String email,
            @NotBlank(message = "이름을 입력해 주세요.") @RequestParam(value = "name") String name,
            @Valid @RequestBody ConfirmAuthCodeRequestDTO confirmAuthCodeRequestDTO) {

        return new BaseResponse<>(authService.confirmPasswordCode(email, name, confirmAuthCodeRequestDTO));
    }

    /**
     * 2.10 비밀번호 변경
     *
     * @param email                  사용자 이메일
     * @param savePasswordRequestDTO 변경하려는 비밀번호
     * @return 비밀번호 변경 성공 메시지
     * @author seonwoo
     * @date 2023-08-02 (수)
     */
    @RequestMapping(value = "/password", method = RequestMethod.POST, params = {"email", "!name"})
    public BaseResponse<String> userPasswordSave(
            @NotBlank(message = "이메일을 입력해 주세요.") @Email(message = "올바르지 않은 이메일 형식입니다.") @RequestParam(value = "email") String email,
            @Valid @RequestBody SavePasswordRequestDTO savePasswordRequestDTO) {

        return new BaseResponse<>(authService.savePassword(email, savePasswordRequestDTO));
    }

    @GetMapping(value = "token")
    public String token(@RequestParam(name = "accessToken") String token, @RequestParam(required = false, name = "authError") String error) {
        if (StringUtils.isNotBlank(error)) {
            return error;
        } else {
            return token;
        }
    }
}

package com.momentum.releaser.domain.user.dto;

import javax.validation.constraints.*;

import lombok.*;

public class AuthRequestDto {
    /**
     * 2.1 회원가입
     *
     * 회원가입 요청 DTO
     */
    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class UserInfoReqestDTO {

        @NotBlank(message = "이름을 입력해주세요.")
        @Size(min = 1, max = 20)
        private String name;

        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "이메일 형식에 맞지 않습니다.")
        private String email;

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Pattern(regexp="(?=.*[0-9])(?=.*[a-zA-Z])(?=.*\\W)(?=\\S+$).{8,20}",
                message = "비밀번호는 영문 대,소문자와 숫자, 특수기호가 적어도 1개 이상씩 포함된 8자 ~ 20자의 비밀번호여야 합니다.")
        private String password;

        @Builder
        public UserInfoReqestDTO(String name, String email, String password) {
            this.name = name;
            this.email = email;
            this.password = password;
        }
    }

    /**
     * 2.2 이메일 로그인
     *
     * 로그인 요청 DTO
     */
    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class UserLoginReqestDTO {

        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "이메일 형식에 맞지 않습니다.")
        private String email;

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Pattern(regexp="(?=.*[0-9])(?=.*[a-zA-Z])(?=.*\\W)(?=\\S+$).{8,20}",
                message = "비밀번호는 영문 대,소문자와 숫자, 특수기호가 적어도 1개 이상씩 포함된 8자 ~ 20자의 비밀번호여야 합니다.")
        private String password;

        @Builder
        public UserLoginReqestDTO(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }

    /**
     * 2.6 이메일 인증
     */
    @Getter
    @NoArgsConstructor
    public static class SendEmailRequestDTO {
        @NotEmpty(message = "이메일을 입력해 주세요.")
        @Email(message = "올바르지 않은 이메일 형식입니다.")
        private String email;

        @Builder
        public SendEmailRequestDTO(String email) {
            this.email = email;
        }
    }

    /**
     * 2.7 이메일 인증 확인
     */
    @Getter
    @NoArgsConstructor
    public static class ConfirmAuthCodeRequestDTO {
        @NotEmpty(message = "인증 코드를 입력해 주세요.")
        private String authCode;

        @Builder
        public ConfirmAuthCodeRequestDTO(String authCode) {
            this.authCode = authCode;
        }
    }

    /**
     * 2.8 비밀번호 인증 메일 전송
     */
    @Getter
    @NoArgsConstructor
    public static class SendEmailForPasswordRequestDTO {

        @NotEmpty(message = "이름을 입력해 주세요.")
        private String name;

        @NotEmpty(message = "이메일을 입력해 주세요.")
        @Email(message = "올바르지 않은 이메일 형식입니다.")
        private String email;

        @Builder
        public SendEmailForPasswordRequestDTO(String name, String email) {
            this.name = name;
            this.email = email;
        }
    }

    /**
     * 2.10 비밀번호 변경
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class SavePasswordRequestDTO {

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Pattern(regexp="(?=.*[0-9])(?=.*[a-zA-Z])(?=.*\\W)(?=\\S+$).{8,20}",
                message = "비밀번호는 영문 대소문자와 숫자, 특수 기호가 적어도 1개 이상씩 포함된 8자 ~ 20자여야 합니다.")
        private String password;

        @NotBlank(message = "확인용 비밀번호를 입력해 주세요.")
        @Pattern(regexp="(?=.*[0-9])(?=.*[a-zA-Z])(?=.*\\W)(?=\\S+$).{8,20}",
                message = "비밀번호는 영문 대소문자와 숫자, 특수 기호가 적어도 1개 이상씩 포함된 8자 ~ 20자여야 합니다.")
        private String confirmPassword;

        @Builder
        public SavePasswordRequestDTO(String password, String confirmPassword) {
            this.password = password;
            this.confirmPassword = confirmPassword;
        }
    }
}

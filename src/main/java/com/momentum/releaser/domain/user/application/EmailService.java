package com.momentum.releaser.domain.user.application;

import javax.mail.MessagingException;

import com.momentum.releaser.domain.user.dto.AuthRequestDto.SendEmailForPasswordRequestDTO;
import com.momentum.releaser.domain.user.dto.AuthRequestDto.SendEmailRequestDTO;

public interface EmailService {

    /**
     * 2.6 이메일 인증
     *
     * @author seonwoo
     * @date 2023-07-31 (월)
     * @return 이메일 인증 코드 메일 전송 성공 메시지
     */
    String sendEmail(SendEmailRequestDTO confirmEmailRequestDTO) throws MessagingException;

    /**
     * 2.8 비밀번호 변경 인증 메일 전송
     *
     * @author seonwoo
     * @date 2023-08-01 (화)
     * @param sendEmailForPasswordRequestDTO 사용자 정보 (이름, 이메일)
     * @return 비밀번호 변경 인증 메일 전송 성공 메시지
     * @throws MessagingException 이메일 전송 및 작성에 문제가 생긴 경우
     */
    String sendEmailForPassword(SendEmailForPasswordRequestDTO sendEmailForPasswordRequestDTO) throws MessagingException;
}

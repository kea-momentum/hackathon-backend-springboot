package com.momentum.releaser.domain.user.application;

import java.util.Optional;
import java.util.Random;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.momentum.releaser.domain.user.dao.UserRepository;
import com.momentum.releaser.domain.user.domain.User;
import com.momentum.releaser.domain.user.dto.AuthRequestDto.SendEmailForPasswordRequestDTO;
import com.momentum.releaser.domain.user.dto.AuthRequestDto.SendEmailRequestDTO;
import com.momentum.releaser.global.exception.CustomException;
import com.momentum.releaser.redis.password.Password;
import com.momentum.releaser.redis.password.PasswordRedisRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import com.momentum.releaser.global.common.property.UrlProperty;
import com.momentum.releaser.redis.RedisUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.momentum.releaser.global.config.BaseResponseStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    // 이메일 인증 시 필요한 인증 코드
    private String authenticationCode;

    @Value("${spring.mail.username}")
    private String userName;

    private final UrlProperty urlProperty;

    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine springTemplateEngine;

    private final UserRepository userRepository;

    private final RedisUtil redisUtil;
    private final PasswordRedisRepository passwordRedisRepository;

    /**
     * 2.6 이메일 인증
     *
     * @author seonwoo
     * @date 2023-07-31 (월)
     */
    @Override
    public String sendEmail(SendEmailRequestDTO confirmEmailRequestDTO) throws MessagingException {
        // Redis에 값이 존재하는지 확인한다.
        deleteIfExistsEmailInRedis(confirmEmailRequestDTO.getEmail());

        // 메일 전송 시 필요한 정보 설정
        MimeMessage emailForm = createEmailForm(
                "[Releaser] 이메일 인증 메일입니다.",
                "이메일 인증",
                confirmEmailRequestDTO.getEmail());

        // 실제 메일 전송
        javaMailSender.send(emailForm);

        // 유효 시간(3분) 동안 {email, authenticationCode} 저장
        redisUtil.setDataExpire(confirmEmailRequestDTO.getEmail(), authenticationCode, 60 * 3L);

        // 인증 코드 반환
        return "이메일 인증 메일이 전송되었습니다.";
    }

    /**
     * 2.8 비밀번호 변경 인증 메일 전송
     *
     * @param sendEmailForPasswordRequestDTO 사용자 정보 (이름, 이메일)
     * @return 비밀번호 변경 인증 메일 전송 성공 메시지
     * @throws MessagingException 이메일 전송 및 작성에 문제가 생긴 경우
     * @author seonwoo
     * @date 2023-08-01 (화)
     */
    @Override
    public String sendEmailForPassword(SendEmailForPasswordRequestDTO sendEmailForPasswordRequestDTO) throws MessagingException {
        // 사용자 정보 유효성 검사
        validateUser(sendEmailForPasswordRequestDTO.getEmail(), sendEmailForPasswordRequestDTO.getName());

        // Redis에 값이 존재하는지 확인한다.
        deleteIfExistsPasswordInRedis(sendEmailForPasswordRequestDTO.getName(), sendEmailForPasswordRequestDTO.getEmail());

        // 메일 전송 시 필요한 정보 설정
        MimeMessage emailForm = createEmailForm(
                "[Releaser] 비밀번호 변경 인증 메일입니다.",
                "비밀번호 변경 인증",
                sendEmailForPasswordRequestDTO.getEmail());

        // 실제 메일 전송
        javaMailSender.send(emailForm);

        // 유효 시간(3분) 동안 {key, value} 저장
        savePasswordToRedis(sendEmailForPasswordRequestDTO.getEmail(), sendEmailForPasswordRequestDTO.getName(), authenticationCode);

        return "비밀번호 변경 인증 메일이 전송되었습니다.";
    }

    // =================================================================================================================

    /**
     * 만약 Redis에 해당 이메일로 된 값이 존재한다면 삭제한다.
     *
     * @param email 사용자가 회원가입 하고자 하는 이메일
     */
    private void deleteIfExistsEmailInRedis(String email) {
        if (redisUtil.existsData(email)) {
            redisUtil.deleteData(email);
        }
    }

    /**
     * 사용자 정보가 유효한지 검사한다.
     *
     * @param email 사용자 이메일
     * @param name  사용자 이름
     */
    private void validateUser(String email, String name) {
        // 만약 존재하지 않는 사용자인 경우 예외를 발생시킨다.
        User user = userRepository.findByEmail(email).orElseThrow(() -> new CustomException(NOT_EXISTS_USER));

        if (!user.getName().equals(name)) {
            // 만약 사용자 이름이 같지 않은 경우 예외를 발생시킨다.
            throw new CustomException(INVALID_USER_NAME);
        }
    }

    /**
     * 만약 Redis에 해당 키 값이 존재한다면 삭제한다.
     *
     * @param name  사용자 이름
     * @param email 사용자 이메일
     */
    private void deleteIfExistsPasswordInRedis(String name, String email) {
        Optional<Password> optionalPassword = passwordRedisRepository.findByNameAndEmail(name, email);
        optionalPassword.ifPresent(passwordRedisRepository::delete);
    }

    /**
     * 이메일 인증을 위한 랜덤 인증 코드 생성
     */
    private void createAuthenticationCode() {
        Random random = new Random();
        StringBuilder key = new StringBuilder();

        for (int i = 0; i < 8; i++) {
            int index = random.nextInt(3);

            switch (index) {
                case 0:
                    key.append((char) (random.nextInt(26) + 97));
                    break;
                case 1:
                    key.append((char) (random.nextInt(26) + 65));
                    break;
                case 2:
                    key.append(random.nextInt(9));
                    break;
            }
        }

        authenticationCode = key.toString();
    }

    /**
     * Thymeleaf 템플릿 엔진에 필요한 값을 주입
     *
     * @param code             이메일 인증 코드
     * @param urlBannerProject 프로젝트 배너 이미지 URL
     * @param urlLogoTeam      팀 로고 이미지 URL
     * @return Thymeleaf 템플릿 엔진을 사용하여 mail.html을 렌더링한 결과
     */
    private String setContext(String subtitle, String code, String urlBannerProject, String urlLogoTeam) {
        Context context = new Context();
        context.setVariable("subtitle", subtitle);
        context.setVariable("code", code);
        context.setVariable("releaser", urlBannerProject);
        context.setVariable("momentum", urlLogoTeam);
        return springTemplateEngine.process("mail", context); // mail.html
    }

    /**
     * 이메일 양식 작성
     *
     * @param email 이메일 인증 코드 메일을 받는 이메일
     * @return 이메일 양식
     * @throws MessagingException 이메일 전송 및 작성에 문제가 생긴 경우
     */
    private MimeMessage createEmailForm(String title, String subtitle, String email) throws MessagingException {
        // 인증 코드 생성
        createAuthenticationCode();

        // 이메일 내용
        String urlBannerProject = urlProperty.getImage().getBannerProject();
        String urlLogoTeam = urlProperty.getImage().getLogoTeam();

        // 이메일 양식 작성을 위한 정보 설정
        MimeMessage message = javaMailSender.createMimeMessage();
        message.addRecipients(MimeMessage.RecipientType.TO, email); // 보내는 이메일 설정
        message.setSubject(title); // 이메일 제목 설정
        message.setFrom(userName); // 보내는 이메일 설정
        message.setText(setContext(subtitle, authenticationCode, urlBannerProject, urlLogoTeam), "utf-8", "html");

        return message;
    }

    /**
     * 사용자 이메일, 이름을 이용하여 비밀번호 인증 코드를 redis에 저장
     *
     * @param email 사용자 이메일 (Redis 키 값)
     * @param name  사용자 이름
     */
    private void savePasswordToRedis(String email, String name, String code) {
        Password password = Password.builder()
                .email(email)
                .name(name)
                .code(authenticationCode)
                .expiredTime(180)
                .build();

        passwordRedisRepository.save(password);
    }
}

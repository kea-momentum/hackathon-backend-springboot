package com.momentum.releaser.domain.user.application;

import static com.momentum.releaser.global.config.BaseResponseStatus.*;

import java.util.Optional;

import com.momentum.releaser.redis.refreshtoken.RefreshTokenRedisRepository;
import org.modelmapper.ModelMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.momentum.releaser.domain.user.dto.AuthRequestDto.ConfirmAuthCodeRequestDTO;
import com.momentum.releaser.domain.user.dto.AuthRequestDto.SavePasswordRequestDTO;
import com.momentum.releaser.domain.user.dto.AuthResponseDto.ConfirmEmailResponseDTO;
import com.momentum.releaser.domain.user.dto.AuthResponseDto.ConfirmPasswordCodeResponseDTO;
import com.momentum.releaser.domain.user.dto.AuthResponseDto.UserInfoResponseDTO;
import com.momentum.releaser.domain.user.mapper.UserMapper;
import com.momentum.releaser.redis.RedisUtil;
import com.momentum.releaser.redis.password.Password;
import com.momentum.releaser.redis.password.PasswordRedisRepository;
import com.momentum.releaser.domain.user.dao.AuthPasswordRepository;
import com.momentum.releaser.domain.user.dao.RefreshTokenRepository;
import com.momentum.releaser.domain.user.dao.UserRepository;
import com.momentum.releaser.domain.user.domain.AuthPassword;
import com.momentum.releaser.domain.user.domain.RefreshToken;
import com.momentum.releaser.domain.user.domain.User;
import com.momentum.releaser.domain.user.dto.AuthRequestDto.UserInfoReqestDTO;
import com.momentum.releaser.domain.user.dto.AuthRequestDto.UserLoginReqestDTO;
import com.momentum.releaser.domain.user.dto.TokenDto;
import com.momentum.releaser.global.exception.CustomException;
import com.momentum.releaser.global.jwt.JwtTokenProvider;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 인증과 관련된 기능을 제공하는 서비스 구현 클래스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final JwtTokenProvider jwtTokenProvider;
    private final ModelMapper modelMapper;

    // Domain
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final AuthPasswordRepository authPasswordRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    // Redis
    private final RedisUtil redisUtil;
    private final PasswordRedisRepository passwordRedisRepository;
    private final RefreshTokenRedisRepository refreshTokenRedisRepository;

    // RabbitMQ
    private final AmqpAdmin rabbitAdmin;
    private final DirectExchange userDirectExchange;
    private final ConnectionFactory connectionFactory;

    /**
     * 2.1 회원가입
     *
     * @author chaeanna
     * @date 2023-07-18
     */
    @Override
    @Transactional
    public UserInfoResponseDTO addSignUpUser(UserInfoReqestDTO userInfoReq) {
        // Email 중복 체크
        validateUniqueEmail(userInfoReq.getEmail());
        // 사용자 정보 저장
        User user = createUser(userInfoReq);
        // 패스워드 암호화 후 저장
        createAndSaveAuthPassword(user, userInfoReq.getPassword());
        // 사용자 이메일에 해당하는 큐를 생성하고, 연결한다.
        createAndBindQueueAndRegisterListener(userInfoReq.getEmail());
        return modelMapper.map(user, UserInfoResponseDTO.class);
    }

    /**
     * 2.2 이메일 로그인
     *
     * @author chaeanna
     * @date 2023-07-18
     */
    @Override
    @Transactional
    public TokenDto saveLoginUser(UserLoginReqestDTO userLoginReq) {
        // 로그인한 유저 정보 저장
        Authentication authentication = authenticateUser(userLoginReq.getEmail(), userLoginReq.getPassword());
        // Token 생성
        TokenDto tokenDto = jwtTokenProvider.generateToken(authentication);
        // Refresh Token 관리
//        manageRefreshToken(userLoginReq.getEmail(), tokenDto.getRefreshToken());
        manageRefreshTokenInRedis(userLoginReq.getEmail(), tokenDto.getRefreshToken());
        return tokenDto;
    }


    /**
     * 2.3 Token 재발급
     *
     * @param accessToken  기존의 Access Token
     * @param refreshToken 새로 발급받은 Refresh Token
     * @throws CustomException Refresh Token이 유효하지 않거나 해당 사용자의 Refresh Token이 존재하지 않을 경우 발생하는 예외 발생
     * @author chaeanna
     * @date 2023-07-19
     */
    @Override
    @Transactional
    public TokenDto saveRefreshUser(String accessToken, String refreshToken) {
        // Refresh Token 검증 및 사용자 이메일 가져오기
//        String email = validateAndGetEmailFromRefreshToken(refreshToken);
        String email = validateAndGetEmailFromRefreshTokenInRedis(refreshToken);

        // Access Token에서 유저 정보 가져오기
        Authentication authentication = validateAndGetAuthenticationFromAccessToken(accessToken);
        // 새로운 Access Token 생성
        String newAccessToken = jwtTokenProvider.generateAccessToken(authentication);
        return TokenDto.builder()
                .grantType("Bearer")
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * 2.7 이메일 인증 확인
     *
     * @param userEmail                 사용자 이메일
     * @param confirmAuthCodeRequestDTO 사용자 이메일 인증 확인 코드
     * @return ConfirmEmailResponseDTO 사용자 이메일
     * @author seonwoo
     * @date 2023-08-02 (수)
     */
    @Override
    public ConfirmEmailResponseDTO confirmEmail(String userEmail, ConfirmAuthCodeRequestDTO confirmAuthCodeRequestDTO) {
        // Redis에 저장된 값과 일치하는지 확인한다.
        int successStatus = verifyEmailAndAuthCode(userEmail, confirmAuthCodeRequestDTO.getAuthCode());

        if (successStatus != 1) {
            // 만약 값이 일치하지 않는다면 예외를 발생시킨다.
            throw new CustomException(INVALID_REDIS_CODE);
        }

        // 만약 일치한다면 이메일 값을 담아 반환한다.
        return ConfirmEmailResponseDTO.builder().email(userEmail).build();
    }

    /**
     * 2.9 비밀번호 변경 인증 확인
     *
     * @param email                     사용자 이메일
     * @param name                      사용자 이름
     * @param confirmAuthCodeRequestDTO 사용자 이메일 인증 확인 코드
     * @return ConfirmPasswordCodeResponseDTO 사용자 이메일, 사용자 이름
     * @author seonwoo
     * @date 2023-08-02 (수)
     */
    @Override
    public ConfirmPasswordCodeResponseDTO confirmPasswordCode(String email, String name, ConfirmAuthCodeRequestDTO confirmAuthCodeRequestDTO) {
        // Redis에 저장된 값과 일치하는지 확인한다.
        verifyAuthCodeWithEmailAndName(email, name, confirmAuthCodeRequestDTO.getAuthCode());

        // 만약 일치한다면 해당하는 사용자 객체를 가져온다.
        User user = findUserByEmailAndName(email, name);

        return UserMapper.INSTANCE.toConfirmPasswordCodeResponseDTO(user);
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
    @Override
    public String savePassword(String email, SavePasswordRequestDTO savePasswordRequestDTO) {
        // 비밀번호와 확인용 비밀번호가 같은지 검증한다.
        verifyPasswordAndConfirmPassword(savePasswordRequestDTO);

        // 사용자 이메일을 이용해 사용자 객체를 가져온다.
        User user = userRepository.findByEmail(email).orElseThrow(() -> new CustomException(NOT_EXISTS_USER));

        // 기존의 비밀번호를 삭제한다.
        deleteExistingPassword(user);

        // 비밀번호를 변경한다.
        createAndSaveAuthPassword(user, savePasswordRequestDTO.getPassword());

        return "비밀번호 변경에 성공하였습니다.";
    }

    // =================================================================================================================

    /**
     * 주어진 이메일이 이미 등록되어 있는지 확인하고, 중복된 이메일일 경우 예외 발생
     *
     * @param email 확인할 이메일
     * @throws CustomException 이미 등록된 이메일인 경우 발생하는 예외
     * @author chaeanna
     * @date 2023-07-18
     */
    private void validateUniqueEmail(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new CustomException(OVERLAP_CHECK_EMAIL);
        }
    }

    /**
     * 사용자 정보를 받아서 새로운 사용자 생성하고 저장
     *
     * @param userInfoReq 사용자 정보 요청 객체
     * @return 생성된 사용자 엔티티
     * @author chaeanna
     * @date 2023-07-18
     */
    private User createUser(UserInfoReqestDTO userInfoReq) {
        return userRepository.save(modelMapper.map(userInfoReq, User.class));
    }

    /**
     * 사용자의 비밀번호를 암호화하여 인증 정보 저장
     *
     * @param user     사용자 엔티티
     * @param password 사용자 비밀번호
     * @author chaeanna
     * @date 2023-07-18
     */
    private void createAndSaveAuthPassword(User user, String password) {
        // 비밀번호를 암호화하여 저장
        String encryptPassword = passwordEncoder.encode(password);

        // 암호화된 비밀번호와 사용자 엔티티로 인증 정보 생성
        AuthPassword authPassword = AuthPassword.builder()
                .user(user)
                .password(encryptPassword)
                .build();

        // 인증 정보를 데이터베이스에 저장
        authPasswordRepository.save(authPassword);
        user.updateAuthPassword(authPassword);
    }

    /**
     * 주어진 이메일과 비밀번호로 사용자 인증하고, 인증 객체 반환
     *
     * @param email    사용자 이메일
     * @param password 사용자 비밀번호
     * @return 인증 객체
     * @author chaeanna
     * @date 2023-07-18
     */
    private Authentication authenticateUser(String email, String password) {
        // 주어진 이메일과 비밀번호로 인증 객체 생성
        Authentication authentication = new UsernamePasswordAuthenticationToken(email, password);

        // 인증 매니저를 통해 인증 처리 후 인증 객체 반환
        return authenticationManagerBuilder.getObject().authenticate(authentication);
    }

    /**
     * 새로 발급받은 Refresh Token 관리
     * <p>
     * 이미 해당 사용자의 Refresh Token이 존재하면 해당 토큰을 업데이트하고 저장합니다.
     * 존재하지 않으면 새로운 토큰을 생성하여 저장합니다.
     *
     * @param email        사용자 이메일
     * @param refreshToken 새로 발급받은 Refresh Token
     * @author chaeanna
     * @date 2023-07-18
     */
    private void manageRefreshToken(String email, String refreshToken) {
        // 해당 사용자의 Refresh Token 찾기
        Optional<RefreshToken> optionalRefreshToken = refreshTokenRepository.findByUserEmail(email);

        // 이미 해당 사용자의 Refresh Token이 존재하면 업데이트
        if (optionalRefreshToken.isPresent()) {
            RefreshToken existingRefreshToken = optionalRefreshToken.get();
            existingRefreshToken.updateToken(refreshToken);
            refreshTokenRepository.save(existingRefreshToken);
        }
        // 존재하지 않으면 새로운 토큰을 생성하여 저장
        else {
            RefreshToken newToken = new RefreshToken(refreshToken, email);
            refreshTokenRepository.save(newToken);
        }
    }

    /**
     * 새로 발급받은 Refresh Token을 Redis에 업데이트한다.
     *
     * @param email        사용자 이메일
     * @param refreshToken 새로 발급받은 Refresh Token
     * @author seonwoo
     * @date 2023-08-15 (화)
     */
    private void manageRefreshTokenInRedis(String email, String refreshToken) {
        // 해당 사용자의 Refresh Token 찾기
        Optional<com.momentum.releaser.redis.refreshtoken.RefreshToken> optionalRefreshToken =
                refreshTokenRedisRepository.findByUserEmail(email);

        // 이미 해당 사용자의 Refresh Token이 존재하면 업데이트한다.
        if (optionalRefreshToken.isPresent()) {
            com.momentum.releaser.redis.refreshtoken.RefreshToken existingRefreshToken = optionalRefreshToken.get();
            existingRefreshToken.updateRefreshToken(refreshToken);
            refreshTokenRedisRepository.save(existingRefreshToken);
        }

        // 존재하지 않는다면 새로운 토큰을 생성하여 저장한다.
        if (optionalRefreshToken.isEmpty()) {
            com.momentum.releaser.redis.refreshtoken.RefreshToken newRefreshToken
                    = new com.momentum.releaser.redis.refreshtoken.RefreshToken(refreshToken, email, 604800);
            refreshTokenRedisRepository.save(newRefreshToken);
        }
    }

    /**
     * 주어진 Refresh Token으로 사용자 이메일 확인하고 반환
     * 만약 Refresh Token이 유효하지 않거나 해당 사용자의 Refresh Token이 존재하지 않을 경우 예외를 발생시킵니다.
     *
     * @param refreshToken 확인할 Refresh Token
     * @return 해당 사용자의 이메일
     * @throws CustomException Refresh Token이 유효하지 않거나 해당 사용자의 Refresh Token이 존재하지 않을 경우 발생하는 예외
     * @author chaeanna
     * @date 2023-07-19
     */
    private String validateAndGetEmailFromRefreshToken(String refreshToken) {
        // Refresh Token에서 사용자 이메일 추출
        String email = jwtTokenProvider.getEmailFromToken(refreshToken);

        // 해당 사용자의 Refresh Token 찾기
        Optional<RefreshToken> existRefreshToken = refreshTokenRepository.findByUserEmail(email);

        // Refresh Token이 유효하지 않거나
        // 해당 사용자의 Refresh Token이 존재하지 않으면 예외 발생
        if (existRefreshToken.isEmpty() || !jwtTokenProvider.validateToken(refreshToken)) {
            throw new CustomException(INVALID_REFRESH_TOKEN);
        }

        return email;
    }

    /**
     * 주어진 Refresh Token으로 사용자 이메일을 확인하고 반환한다.
     * 만약 Refresh Token이 유효하지 않거나 해당 사용자의 Refresh Token이 존재하지 않을 경우 예외를 발생시킨다.
     *
     * @param refreshToken 확인할 Refresh Token 값
     * @return 해당 사용자의 이메일
     * @throws CustomException Refresh Token이 유효하지 않거나 해당 사용자의 Refresh Token이 존재하지 않을 경우 발생하는 예외
     * @author seonwoo
     * @date 2023-08-15 (화)
     */
    private String validateAndGetEmailFromRefreshTokenInRedis(String refreshToken) {
        // Refresh Token에서 사용자 이메일 추출
        String email = jwtTokenProvider.getEmailFromToken(refreshToken);

        // 해당 사용자의 Refresh Token 찾기
        Optional<com.momentum.releaser.redis.refreshtoken.RefreshToken> existingRefreshToken
                = refreshTokenRedisRepository.findByUserEmail(email);

        // Refresh Token이 유효하지 않거나 해당 사용자의 Refresh Token이 존재하지 않으면 예외 발생
        if (existingRefreshToken.isEmpty() || !jwtTokenProvider.validateToken(refreshToken)) {
            throw new CustomException(INVALID_REFRESH_TOKEN);
        }

        return email;
    }

    /**
     * 주어진 Access Token으로 사용자 인증 객체 확인하고 반환
     *
     * @param accessToken 확인할 Access Token
     * @return 인증 객체 (Authentication)
     * @author chaeanna
     * @date 2023-07-19
     */
    private Authentication validateAndGetAuthenticationFromAccessToken(String accessToken) {
        // 주어진 Access Token으로 사용자 인증 객체 가져옴
        return jwtTokenProvider.getAuthentication(accessToken);
    }

    /**
     * Redis에 저장된 이메일과 인증 코드 값이 올바른지 확인한다.
     *
     * @param email    사용자 이메일
     * @param authCode 사용자 이메일 인증 코드
     * @return 이메일 인증 성공 여부
     * @author seonwoo
     * @date 2023-08-01 (화)
     */
    private int verifyEmailAndAuthCode(String email, String authCode) {
        // 사용자 이메일 키 값을 가지고 인증 코드를 가져온다.
        String savedAuthCode = redisUtil.getData(email);

        if (savedAuthCode == null) {
            // 만약 유효 시간이 만료되었다면 예외를 발생시킨다.
            throw new CustomException(NOT_EXISTS_REDIS_CODE);
        }

        // 인증 코드가 동일한지 비교한다.
        return savedAuthCode.equals(authCode) ? 1 : 0;
    }

    /**
     * Redis에 저장된 인증 코드와 동일한지 확인한다.
     *
     * @param email 사용자 이메일
     * @param name  사용자 이름
     * @author seonwoo
     * @date 2023-08-02 (수)
     */
    private void verifyAuthCodeWithEmailAndName(String email, String name, String code) {
        // Redis의 키 값(email, name)을 이용하여 저장된 인증 코드 값을 가져온다.
        Password password = passwordRedisRepository.findById(email).orElseThrow(() -> new CustomException(INVALID_REDIS_KEY));
        log.info("password.getCode: {}, code: {}", password.getCode(), code);

        // 만약 사용자 이름이 일치하지 않다면 예외를 발생시킨다.
        if (!password.getName().equals(name)) {
            throw new CustomException(INVALID_USER_NAME);
        }

        // 인증 코드가 일치하지 않다면 예외를 발생시킨다.
        if (!password.getCode().equals(code)) {
            throw new CustomException(INVALID_REDIS_CODE);
        }
    }

    /**
     * 사용자 이메일과 이름을 이용하여 데이터베이스에 저장된 객체를 가져온다.
     *
     * @param email 사용자 이메일
     * @param name  사용자 이름
     * @return user 사용자 객체
     * @author seonwoo
     * @date 2023-08-02 (수)
     */
    private User findUserByEmailAndName(String email, String name) {
        // 이메일을 이용하여 사용자 객체를 가져온다.
        User user = userRepository.findByEmail(email).orElseThrow(() -> new CustomException(NOT_EXISTS_USER));

        // 만약 정보가 다르다면 예외를 발생시킨다.
        if (!user.getName().equals(name)) {
            throw new CustomException(INVALID_USER_NAME);
        }

        // 정보가 다 같은 경우 사용자 객체를 반환한다.
        return user;
    }

    /**
     * 사용자가 변경하려고 하는 비밀번호와 확인용 비밀번호가 같은지 검사한다.
     * 다르다면 예외를 발생시킨다.
     *
     * @param savePasswordRequestDTO 비밀번호, 확인용 비밀번호
     * @author seonwoo
     * @date 2023-08-02 (수)
     */
    private void verifyPasswordAndConfirmPassword(SavePasswordRequestDTO savePasswordRequestDTO) {
        String password = savePasswordRequestDTO.getPassword();
        String confirmPassword = savePasswordRequestDTO.getConfirmPassword();

        if (!password.equals(confirmPassword)) {
            throw new CustomException(NOT_EQUAL_PASSWORD_AND_CONFIRM_PASSWORD);
        }
    }

    /**
     * 이미 존재하는 비밀번호를 삭제한다.
     *
     * @param user 사용자 객체
     * @author seonwoo
     * @date 2023-08-02 (수)
     */
    private void deleteExistingPassword(User user) {
        AuthPassword authPassword = authPasswordRepository.findByUser(user);

        if (authPassword != null) {
            // 기존의 비밀번호 정보를 삭제한다.
            authPasswordRepository.delete(authPassword);
        }
    }

    /**
     * 사용자가 가입할 때 큐를 생성하고 바인딩하는 메서드
     *
     * @param userEmail 사용자 이메일
     */
    private void createAndBindQueueAndRegisterListener(String userEmail) {
        String queueName = "releaser.user." + userEmail;
        String routingKey = "releaser.user." + userEmail;

        createUserQueue(queueName);
        bindUserQueue(queueName, routingKey);
//        registerListener(queueName);
    }

    /**
     * 개별 사용자 큐를 생성한다.
     *
     * @param queueName 큐 이름
     * @author seonwoo
     * @date 2023-08-07 (월)
     */
    private void createUserQueue(String queueName) {
        Queue queue = new Queue(queueName, true, false, false);
        rabbitAdmin.declareQueue(queue);
    }

    /**
     * 생성한 사용자 큐를 바인딩한다.
     *
     * @param queueName  큐 이름
     * @param routingKey 라우팅 키
     * @author seonwoo
     * @date 2023-08-07 (월)
     */
    private void bindUserQueue(String queueName, String routingKey) {
        Binding binding = BindingBuilder.bind(new Queue(queueName)).to(userDirectExchange).with(routingKey);
        rabbitAdmin.declareBinding(binding);
    }

    /**
     * 리스너를 등록한다.
     *
     * @param queueName 큐 이름
     * @author seonwoo
     * @date 2023-08-07 (월)
     */
//    private void registerListener(String queueName) {
//        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
//        container.setConnectionFactory(connectionFactory);
//        container.setQueueNames(queueName);
//        container.setMessageListener(new MessageListenerAdapter(this, "receiveMessagePerUser"));
//        container.start();
//    }
}

package com.momentum.releaser.domain.user.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import com.momentum.releaser.redis.refreshtoken.RefreshTokenRedisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.momentum.releaser.domain.user.dao.AuthPasswordRepository;
import com.momentum.releaser.domain.user.dao.RefreshTokenRepository;
import com.momentum.releaser.domain.user.dao.UserRepository;
import com.momentum.releaser.domain.user.domain.AuthPassword;
import com.momentum.releaser.domain.user.domain.User;
import com.momentum.releaser.domain.user.dto.AuthRequestDto.UserInfoReqestDTO;
import com.momentum.releaser.domain.user.dto.AuthResponseDto.UserInfoResponseDTO;
import com.momentum.releaser.global.jwt.JwtTokenProvider;
import com.momentum.releaser.redis.RedisUtil;
import com.momentum.releaser.redis.password.PasswordRedisRepository;

class AuthServiceImplTest {

    private AuthServiceImpl authService;
    private PasswordEncoder passwordEncoder;
    private UserRepository userRepository;
    private AuthPasswordRepository authPasswordRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private AuthenticationManagerBuilder authenticationManagerBuilder;
    private JwtTokenProvider jwtTokenProvider;
    private ModelMapper modelMapper;
    private RedisUtil redisUtil;
    private PasswordRedisRepository passwordRedisRepository;
    private RefreshTokenRedisRepository refreshTokenRedisRepository;
    private AmqpAdmin rabbitAdmin;
    private DirectExchange userDirectExchange;
    private ConnectionFactory connectionFactory;


    @BeforeEach
    void setUp() {
        authenticationManagerBuilder = mock(AuthenticationManagerBuilder.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        modelMapper = mock(ModelMapper.class);

        passwordEncoder = mock(PasswordEncoder.class);
        userRepository = mock(UserRepository.class);
        authPasswordRepository = mock(AuthPasswordRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);

        redisUtil = mock(RedisUtil.class);
        passwordRedisRepository = mock(PasswordRedisRepository.class);
        refreshTokenRedisRepository = mock(RefreshTokenRedisRepository.class);

        rabbitAdmin = mock(AmqpAdmin.class);
        userDirectExchange = mock(DirectExchange.class);
        connectionFactory = mock(ConnectionFactory.class);

        authService = new AuthServiceImpl(
                authenticationManagerBuilder,
                jwtTokenProvider,
                modelMapper,
                passwordEncoder,
                userRepository,
                authPasswordRepository,
                refreshTokenRepository,
                redisUtil,
                passwordRedisRepository,
                refreshTokenRedisRepository,
                rabbitAdmin,
                userDirectExchange,
                connectionFactory);
    }

    @Test
    @DisplayName("2.1 회원가입")
    void testAddSignUpUser() {
        // 테스트를 위한 mock 회원가입 정보 생성
        UserInfoReqestDTO mockReqDTO = new UserInfoReqestDTO(
                "testUser", "testUser@releaser.com", "password"
        );
        User mockUser = new User(
                "testUser", "testUser@releaser.com", null, 'Y'
        );
        AuthPassword mockPassword = new AuthPassword(
                mockUser, "encryptedPassword", 'Y'
        );
        UserInfoResponseDTO userResDTO = new UserInfoResponseDTO(
                1L, "testUser", "testUser@releaser.com"
        );

        // userRepository.findByEmail() 메서드가 빈 결과를 반환하도록 설정 -> 회원이 아닌 경우
        when(userRepository.findByEmail(mockReqDTO.getEmail())).thenReturn(Optional.empty());

        // userRepository.save() 메서드가 mock 유저를 반환하도록 설정
        when(userRepository.save(modelMapper.map(mockReqDTO, User.class))).thenReturn(mockUser);

        // passwordEncoder.encode() 메서드가 mock 비밀번호를 반환하도록 설정
        when(passwordEncoder.encode(mockReqDTO.getPassword())).thenReturn("encryptedPassword");

        // authPasswordRepository.save() 메서드가 mock 비밀번호 정보를 반환하도록 설정
        when(authPasswordRepository.save(any())).thenReturn(mockPassword);

        // modelMapper.map() 메서드가 mock 회원 정보를 반환하도록 설정
        when(modelMapper.map(mockUser, UserInfoResponseDTO.class)).thenReturn(userResDTO);

        // 회원가입 서비스 호출
        UserInfoResponseDTO result = authService.addSignUpUser(mockReqDTO);

        // 예상된 결과와 실제 결과 비교
        assertEquals(userResDTO.getName(), result.getName());
        assertEquals(userResDTO.getEmail(), result.getEmail());

        // 각 메서드가 호출됐는지 확인
        verify(userRepository, times(1)).findByEmail(mockReqDTO.getEmail());
        verify(userRepository, times(1)).save(modelMapper.map(mockReqDTO, User.class));
        verify(passwordEncoder, times(1)).encode(mockReqDTO.getPassword());
        verify(authPasswordRepository, times(1)).save(any(AuthPassword.class));
        verify(modelMapper, times(1)).map(mockUser, UserInfoResponseDTO.class);
    }

}

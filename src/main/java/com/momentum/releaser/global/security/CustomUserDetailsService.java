package com.momentum.releaser.global.security;


import com.momentum.releaser.domain.user.dao.AuthPasswordRepository;
import com.momentum.releaser.domain.user.dao.UserRepository;
import com.momentum.releaser.domain.user.domain.AuthPassword;
import com.momentum.releaser.global.config.BaseResponseStatus;
import com.momentum.releaser.global.exception.CustomException;
import com.momentum.releaser.global.jwt.UserPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Collection;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    private final AuthPasswordRepository authPasswordRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        com.momentum.releaser.domain.user.domain.User checkUser = userRepository.findOneByEmail(email).orElseThrow(() -> new CustomException(BaseResponseStatus.NOT_EXISTS_USER));
        AuthPassword authPassword = authPasswordRepository.findByUser(checkUser);

        return UserPrincipal.create(checkUser, authPassword);
    }

//    @Override
//    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
//        try {
//            com.momentum.releaser.domain.user.domain.User checkUser = userRepository.findOneByEmail(email).orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
//            AuthPassword authPassword = authPasswordRepository.findByUser(checkUser);
//
//            return createUserDetails(checkUser, authPassword);
//        } catch (ResourceNotFoundException ex) {
//            throw new UsernameNotFoundException(ex.getMessage(), ex);
//        } catch (CustomException ex) {
//            // CustomException 처리 로직
//            throw new UsernameNotFoundException(ex.getMessage(), ex);
//        }
//    }


    // 해당하는 User 의 데이터가 존재한다면 UserDetails 객체로 만들어서 리턴

    /**
     * 여기서 PasswordEncoder를 통해 UserDetails 객체를 생성할 때 encoding을 해줌 => 왜냐하면 Spring Security는 사용자 검증을 위해 encoding된 password와 그렇지 않은 password를 비교하기 때문
     * 실제로는 DB 자체에 encoding된 password 값을 갖고 있고 그냥 memer.getPassword()로 encoding된 password를 꺼내는 것이 좋지만, 예제에서는 편의를 위해 검증 객체를 생성할 때 encoding을 해줌.
     */
    private UserDetails createUserDetails(com.momentum.releaser.domain.user.domain.User checkUser, AuthPassword authPassword) {

        Collection<GrantedAuthority> authorities = Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN")); // 사용자의 권한 정보 설정


        UserDetails userDetails = User.builder()
                .username(checkUser.getEmail())
                .password(authPassword.getPassword())
                .authorities(authorities)
                .build();
        return userDetails;
    }
}
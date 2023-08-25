package com.momentum.releaser.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.momentum.releaser.domain.user.dao.AuthPasswordRepository;
import com.momentum.releaser.domain.user.dao.AuthSocialRepository;
import com.momentum.releaser.domain.user.dao.UserRepository;
import com.momentum.releaser.global.config.oauth2.*;
import com.momentum.releaser.global.exception.CustomLogoutSuccessHandler;
import com.momentum.releaser.global.jwt.JwtAuthenticationFilter;
import com.momentum.releaser.global.jwt.JwtTokenProvider;
import com.momentum.releaser.global.security.CustomAccessDeniedHandler;
import com.momentum.releaser.global.security.CustomAuthenticationEntryPoint;
import com.momentum.releaser.global.security.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


import lombok.RequiredArgsConstructor;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * SecurityConfig는 Spring Security 설정을 위한 클래스.
 *
 * @author rimsong
 */

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableGlobalMethodSecurity(securedEnabled = true) // @Secured 어노테이션 활성화!!
public class SecurityConfig {

    private final ObjectMapper objectMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final UserRepository userRepository;
    private final AuthPasswordRepository authPasswordRepository;
    private final AuthSocialRepository authSocialRepository;
    private final CustomUserDetailsService customUserDetailsService;
    private final CustomLogoutSuccessHandler customLogoutSuccessHandler;
    private final AppProperties appProperties;


    /**
     * 정적 리소스(/resources)가 Spring Security 필터에 걸리지 않도록 설정한다.
     *
     * @return WebSecurityCustomizer
     */
//    @Bean
//    public WebSecurityCustomizer configure() {
//        return (web) -> web.ignoring().antMatchers("/images/**");
//    }
    @Bean
    OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler() {
        return new OAuth2AuthenticationSuccessHandler(jwtTokenProvider, appProperties, httpCookieOAuth2AuthorizationRequestRepository());
    }

//    @Bean
//    CustomUserDetailsService customUserDetailsService(){
//        return new CustomUserDetailsService(userRepository, authPasswordRepository);
//    }

    @Bean
    CustomOAuth2UserService customOAuth2UserService() {
        return new CustomOAuth2UserService(userRepository, authSocialRepository, authPasswordRepository);
    }

    @Bean
    OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler() {
        return new OAuth2AuthenticationFailureHandler(httpCookieOAuth2AuthorizationRequestRepository());
    }

    @Bean
    HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository() {
        return new HttpCookieOAuth2AuthorizationRequestRepository();
    }

    @Bean
    AuthenticationManager authenticationManager() {
        return new ProviderManager(authenticationProvider());
    }

    @Bean
    AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        authenticationProvider.setUserDetailsService(customUserDetailsService);
        return authenticationProvider;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .cors()
                .and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and().csrf().disable().formLogin().disable().httpBasic().disable().exceptionHandling().authenticationEntryPoint(new RestAuthenticationEntryPoint())
                .and().authorizeRequests()
                .antMatchers("/oauth2/**", "/login/**", "/api/auth/**", "/notification/**").permitAll()
                .anyRequest()
                .authenticated();

        http
                .logout()
                .logoutUrl("/api/auth/logout") // 로그아웃 URL을 지정합니다.
                .logoutSuccessHandler(customLogoutSuccessHandler) // 로그아웃 성공 후의 처리를 위해 CustomLogoutSuccessHandler를 등록합니다.
                .deleteCookies("JSESSIONID") // 로그아웃 시 삭제할 쿠키를 지정합니다.
                .clearAuthentication(true) // 인증 정보를 삭제합니다.
                .invalidateHttpSession(true) // 세션을 무효화합니다.
                .permitAll(); // 로그아웃은 모두에게 허용합니다.

        http
                .oauth2Login()
                .authorizationEndpoint()
                .baseUri("/oauth2/authorize")
                .authorizationRequestRepository(httpCookieOAuth2AuthorizationRequestRepository())
                .and()
                .redirectionEndpoint()
                .baseUri("/login/oauth2/code/*")
                .and()
                .userInfoEndpoint()
                .userService(customOAuth2UserService())
                .and()
                .successHandler(oAuth2AuthenticationSuccessHandler())
                .failureHandler(oAuth2AuthenticationFailureHandler());

//        http
//                .oauth2Login()
//                .authorizationEndpoint()
//                .baseUri("/oauth2/authorize")
//                .authorizationRequestRepository(cookieAuthorizationRequestRepository)
//                .and()
//                .redirectionEndpoint()
//                .baseUri("/oauth2/callback/*")
//
//                .and()
//                .userInfoEndpoint()
//                .userService(customOAuth2UserService())
//                .and()
//                .successHandler(oAuth2AuthenticationSuccessHandler())
//                .failureHandler(oAuth2AuthenticationFailureHandler());

        http
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        // 401 Error 처리, Authorization 즉, 인증과정에서 실패할 시 처리
        http.exceptionHandling().authenticationEntryPoint(customAuthenticationEntryPoint);

        // 403 Error 처리, 인증과는 별개로 추가적인 권한이 충족되지 않는 경우
        http.exceptionHandling().accessDeniedHandler(customAccessDeniedHandler);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}

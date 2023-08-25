package com.momentum.releaser.global.exception;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.momentum.releaser.global.security.SecurityExceptionDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    private final ObjectMapper objectMapper;

    public CustomLogoutSuccessHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        // 로그아웃 성공에 대한 JSON 응답을 전송합니다.
        String message = "로그아웃에 성공하였습니다.";
        SecurityExceptionDto exceptionDto = new SecurityExceptionDto(HttpStatus.OK.value(), message);
        String jsonResponse = objectMapper.writeValueAsString(exceptionDto);

        response.setCharacterEncoding(StandardCharsets.UTF_8.toString()); // Set the character encoding
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}

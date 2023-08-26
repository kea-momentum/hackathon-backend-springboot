package com.momentum.releaser.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.momentum.releaser.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

import static com.momentum.releaser.global.config.BaseResponseStatus.SERVER_ERROR;

@Slf4j
@RequiredArgsConstructor
@Component
public class ReqResFilter extends OncePerRequestFilter {
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // Trace id를 생성한다.
        String traceId = UUID.randomUUID().toString();

        try {
            CachedBodyHttpServletRequest requestWrapper = new CachedBodyHttpServletRequest(request);
            requestWrapper.setAttribute("traceId", traceId);
            // 생성한 tracie id를 request attribute에 추가한 후 필터로 넘긴다.
            filterChain.doFilter(requestWrapper, response);
        } catch (Exception e) {
            log.error(e.toString());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());

            try {
                try (PrintWriter writer = response.getWriter()) {
                    writer.print(objectMapper.writeValueAsString(new CustomException(SERVER_ERROR)));
                    writer.flush();
                }
            } catch (IOException exception) {
                log.warn("IOException occurred.");
//                throw new RuntimeException();RuntimeException
            }
        }
    }
}

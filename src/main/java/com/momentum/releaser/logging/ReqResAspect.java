package com.momentum.releaser.logging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.momentum.releaser.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Before;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static com.momentum.releaser.global.config.BaseResponseStatus.SERVER_ERROR;

/**
 * Spring context 외부에 있는 필터를 확인하기 위해 AOP를 사용한다.
 * 컨트롤러 메서드가 실행되기 직전과 이후의 정보를 저장하여 에러의 위치를 디테일하게 확인할 수 있다.
 */
@Slf4j
@RequiredArgsConstructor
@Aspect
@Component
@Order(1)
public class ReqResAspect {
    private final ObjectMapper objectMapper;

    @Bean
    public RequestContextListener requestContextListener() {
        return new RequestContextListener();
    }

    //    @Before("execution(* com.momentum.releaser..*.*(..))")
    @Pointcut("within(com.momentum.releaser..*)")
    void restApiPointCut() {
    }

    @Pointcut("within(org.springframework.stereotype.Repository) *)"
            + " || within(org.springframework.stereotype.Service) *)"
            + "|| within(org.springframework.stereotype.Component) *)"
            + " || within(org.springframework.web.bind.annotation.RestController) *)")
    public void springBeanPointcut() {
    }

    /**
     * @param joinPoint Spring AOP에서 조인 포인트의 단위는 메서드만 해당한다. 즉, 해당 메서드라고 이해하면 쉽다.
     * @return
     * @throws Throwable
     */
    @Around("restApiPointCut() && springBeanPointcut()")
    Object reqResLogging(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        // fields
        String traceId = (String) request.getAttribute("traceId");
        String className = joinPoint.getSignature().getDeclaringTypeName(); // 해당 메서드가 선언된 클래스 이름
        String methodName = joinPoint.getSignature().getName(); // 해당 메서드의 이름
        Map<String, String> params = getParams(request); // Query string 파라미터를 맵의 형태로 변환하는 메서드
        String deviceType = request.getHeader("x-custom-device-type");
        String serverIp = InetAddress.getLocalHost().getHostAddress();

        ReqResLogging reqResLogging = ReqResLogging.builder()
                .traceId(traceId)
                .className(className)
                .httpMethod(request.getMethod())
                .uri(request.getRequestURI())
                .method(methodName)
                .params(params)
                .logTime(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .serverIp(serverIp)
                .deviceType(deviceType)
                .requestBody(objectMapper.readTree(request.getInputStream().readAllBytes()))
                .build();

        long start = System.currentTimeMillis(); // 메서드 진행 경과 시간
        try {
            Object result = joinPoint.proceed(); // 조인 포인트 로직을 실행시키며, 반환 값은 해당 메서드의 반환 값이다.
            long elapsedTime = System.currentTimeMillis() - start;
            String elapsedTimeStr = "Method: " + className + "." + methodName + "() execution time: " + elapsedTime + "ms";

            ReqResLogging logging;
            if (result instanceof ResponseEntity) {
//                ResponseEntity<?> responseEntity = (ResponseEntity<?>) result;

                // 프로퍼티의 값을 바꾸지 않고 새로운 객체를 만들어서 값을 변경한다.
                logging = reqResLogging.copyWithOther((JsonNode) ((ResponseEntity<?>) result).getBody(), elapsedTimeStr);
            } else {
                logging = reqResLogging.copyWithOther(null, null);
            }

            // 로그 출력
            log.info(objectMapper.writeValueAsString(logging));
            return result;

        } catch (Exception e) {
            // AOP는 오직 로깅 목적이므로 response를 여기서 직접 작성하지 않는다.
            throw new CustomException(SERVER_ERROR);

//            log.info(
//                    "{}",
//                    objectMapper.writeValueAsString(
//                            reqResLogging.copyWithOther(
//                                    (JsonNode) new CustomException(
//                                            HttpStatus.INTERNAL_SERVER_ERROR,
//                                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
//                                            traceId,
//                                            "서버에 일시적인 장애가 있습니다."
//                                    ),
//                                    null
//                            )
//                    )
//            );
//            throw e;
        }
    }

    private Map<String, String> getParams(HttpServletRequest request) {
        Map<String, String> jsonObject = new HashMap<>();
        Enumeration<String> paramNames = request.getParameterNames();

        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String replaceParam = paramName.replace("\\.", "-");
            jsonObject.put(replaceParam, request.getParameter(paramName));
        }

        return jsonObject;
    }
}
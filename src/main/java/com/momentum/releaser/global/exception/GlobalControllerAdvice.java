package com.momentum.releaser.global.exception;

import com.momentum.releaser.global.config.BaseException;
import com.momentum.releaser.global.config.BaseResponse;
import com.momentum.releaser.global.config.BaseResponseStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import javax.validation.ConstraintViolationException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.momentum.releaser.global.config.BaseResponseStatus.INVALID_QUERY_STRING;
import static com.momentum.releaser.global.config.BaseResponseStatus.INVALID_REQUEST_BODY;


/**
 * 전역 처리 Global Exception Handling
 * - 에러 추가 방법: @ExceptionHandler로 처리하고 싶은 예외를 처리할 수 있도록 하면 된다.
 * - 사용 방법: 원하는 컨트롤러에서 throw new ExceptionName(Argument); 작성해주면 된다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalControllerAdvice {

    /**
     * BaseException에서 발생하는 예외를 처리한다.
     */
    @ExceptionHandler(value = BaseException.class)
    protected BaseResponse<BaseResponseStatus> handleBaseException(BaseException e) {
        return new BaseResponse<>(e.getStatus());
    }

    /**
     * (BaseException을 제외한) Exception 클래스의 자식들의 모든 예외를 처리한다.
     * 이 경우는 Response Body뿐만 아니라 HTTP Status Code에도 에러를 명시하도록 @ResponseStatus를 사용한다.
     */
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = Exception.class)
    protected String handleException(Exception e) {
        return e.getMessage();
    }

    @ExceptionHandler({ CustomException.class })
    protected BaseResponse handleCustomException(CustomException e) {
        if (e.getReleaseId() != null) {
            Map<String, Long> error = new HashMap<>();
            error.put("releaseId", e.getReleaseId());
            return new BaseResponse<>(e.getExceptionStatus(), error);
        } else if (e.getInviteProjectMemberRes() != null) {
            Map<String, String> error = new HashMap<>();
            error.put("projectId", Long.toString(e.getInviteProjectMemberRes().getProjectId()));
            error.put("projectName", e.getInviteProjectMemberRes().getProjectName());
            return new BaseResponse<>(e.getExceptionStatus(), error);
        } else {
            return new BaseResponse<>(e.getExceptionStatus());
        }
    }

    /**
     * Validation 검증 에러 (Request body)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<Map<String, String>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.debug(e.getMessage());

        Map<String, String> errors = new HashMap<>();

        e.getBindingResult().getAllErrors()
                .forEach(c -> errors.put(((FieldError) c).getField(), c.getDefaultMessage()));

        return new BaseResponse<>(INVALID_REQUEST_BODY, errors);
    }

    /**
     * Validation 검증 에러 (QueryString)
     */
    @ExceptionHandler(BindException.class)
    public BaseResponse<Map<String, String>> handleBindException(BindException e) {
        log.debug(e.getMessage());

        Map<String, String> errors = new HashMap<>();

        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName;
            if (error instanceof FieldError) {
                fieldName = ((FieldError) error).getField();
            } else {
                fieldName = error.getObjectName();
            }

            // 태그 필드인 경우 특별한 메시지 처리
            if ("tag".equals(fieldName)) {
                errors.put(fieldName, "태그 타입은 DEPRECATED, CHANGED, NEW, FEATURE, FIXED 중 하나여야 합니다.");
            } else if ("managerId".equals(fieldName)) {
                errors.put(fieldName, "담당자 식별 번호는 양수만 가능합니다.");
            } else if ("startReleaseVersion".equals(fieldName) || "endReleaseVersion".equals(fieldName) || "startVersion".equals(fieldName) || "endVersion".equals(fieldName)) {
                errors.put(fieldName, "릴리즈 버전 형식에 맞지 않습니다.");
            } else {
                errors.put(fieldName, "날짜 형식이 맞지 않습니다.");
            }
        });

        return new BaseResponse<>(INVALID_QUERY_STRING, errors);
    }

    /**
     * Validation 검증 에러
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public BaseResponse<Map<String, String>> handleConstraintViolationException(ConstraintViolationException e) {
        log.debug(e.getMessage());

        Map<String, String> errors = new HashMap<>();

        e.getConstraintViolations()
                .parallelStream()
                .forEach(violation -> {
                    String fieldName = violation.getPropertyPath().toString().split("\\.")[1];
                    errors.put(fieldName, violation.getMessage());
                });

        return new BaseResponse<>(INVALID_REQUEST_BODY, errors);
    }

    /**
     * 서버 에러
     */
    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<String> handleRuntimeException(RuntimeException e) {
        log.debug(e.getMessage());
        return new BaseResponse<>(BaseResponseStatus.SERVER_ERROR);
    }

    /**
     * 데이터베이스 에러
     */
    @ExceptionHandler(SQLException.class)
    public BaseResponse<String> handleSQLException(SQLException e) {
        return new BaseResponse<>(BaseResponseStatus.DATABASE_ERROR);
    }

    /**
     * 입출력 에러
     */
    @ExceptionHandler(IOException.class)
    public BaseResponse<String> handleIOException(IOException e) {
        log.debug(e.getMessage());
        return new BaseResponse<>(BaseResponseStatus.IO_ERROR);
    }
}

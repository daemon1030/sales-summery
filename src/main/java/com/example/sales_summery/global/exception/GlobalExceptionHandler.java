package com.example.sales_summery.global.exception;

import com.example.sales_summery.global.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

// Controller 밖으로 전달된 예외를 공통 실패 응답으로 변환한다.
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.name(), exception.getMessage()));
    }

    // @Valid가 적용된 JSON 요청 DTO의 첫 번째 필드 오류를 반환한다.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception
    ) {
        return validationError(firstFieldErrorMessage(exception));
    }

    // 쿼리 파라미터를 객체로 바인딩할 때 발생한 검증 오류를 처리한다.
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException exception) {
        return validationError(firstFieldErrorMessage(exception));
    }

    // 타입 변환과 잘못된 JSON은 내부 구현을 노출하지 않고 일반 검증 오류로 처리한다.
    @ExceptionHandler({ConstraintViolationException.class, HandlerMethodValidationException.class,
            MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<Void>> handleInvalidRequest(Exception exception) {
        return validationError(ErrorCode.VALIDATION_ERROR.getMessage());
    }

    // 예상하지 못한 예외는 서버 로그에만 남기고 안전한 메시지를 반환한다.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        log.error("Unhandled exception", exception);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.name(), errorCode.getMessage()));
    }

    private ResponseEntity<ApiResponse<Void>> validationError(String message) {
        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.name(), message));
    }

    private String firstFieldErrorMessage(BindException exception) {
        return exception.getBindingResult().getFieldErrors().stream().findFirst()
                .map(FieldError::getDefaultMessage).filter(Objects::nonNull)
                .orElse(ErrorCode.VALIDATION_ERROR.getMessage());
    }
}

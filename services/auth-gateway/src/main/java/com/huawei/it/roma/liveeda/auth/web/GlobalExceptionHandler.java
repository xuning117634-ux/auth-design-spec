package com.huawei.it.roma.liveeda.auth.web;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(GatewayException.class)
    public ResponseEntity<ApiErrorResponse> handleGatewayException(GatewayException exception) {
        log.warn("gateway exception handled, status={}, message={}",
                exception.getStatus(), exception.getMessage());
        return ResponseEntity.status(exception.getStatus())
                .body(new ApiErrorResponse(exception.getStatus().name(), exception.getMessage()));
    }

    @ExceptionHandler({ConstraintViolationException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<ApiErrorResponse> handleValidation(Exception exception) {
        log.warn("validation exception handled, type={}, message={}",
                exception.getClass().getSimpleName(), exception.getMessage());
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse("BAD_REQUEST", exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        log.warn("illegal argument exception handled, message={}", exception.getMessage());
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse("BAD_REQUEST", exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
        log.error("unexpected exception handled, type={}", exception.getClass().getSimpleName(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse("INTERNAL_SERVER_ERROR", "Unexpected gateway error"));
    }
}

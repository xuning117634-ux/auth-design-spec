package com.huawei.it.roma.liveeda.demoagent.controller;

import com.huawei.it.roma.liveeda.demoagent.web.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class DemoAgentExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handle(ResponseStatusException exception) {
        String message = exception.getReason() == null || exception.getReason().isBlank()
                ? "请求处理失败"
                : exception.getReason();
        return ResponseEntity.status(exception.getStatusCode())
                .body(new ErrorResponse("REQUEST_FAILED", message));
    }
}

package com.huawei.it.roma.liveeda.auth.web;

import org.springframework.http.HttpStatus;

public class GatewayException extends RuntimeException {

    private final HttpStatus status;

    public GatewayException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

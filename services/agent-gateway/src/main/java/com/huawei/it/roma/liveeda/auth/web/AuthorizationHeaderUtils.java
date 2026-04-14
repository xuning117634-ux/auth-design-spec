package com.huawei.it.roma.liveeda.auth.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

public final class AuthorizationHeaderUtils {

    private AuthorizationHeaderUtils() {
    }

    public static String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new GatewayException(HttpStatus.UNAUTHORIZED, "Missing Bearer token");
        }
        return authorizationHeader.substring("Bearer ".length()).trim();
    }
}

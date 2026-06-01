package com.huawei.it.roma.liveeda.auth.web;

public record TrCookieResolveResponse(
        boolean found,
        String cookie,
        Long expiresIn
) {
}

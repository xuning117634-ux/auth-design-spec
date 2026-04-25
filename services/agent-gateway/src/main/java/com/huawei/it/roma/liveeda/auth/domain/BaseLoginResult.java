package com.huawei.it.roma.liveeda.auth.domain;

public record BaseLoginResult(
        String userId,
        String uuid,
        String username
) {
    public BaseLoginResult(String userId, String username) {
        this(userId, userId, username);
    }
}

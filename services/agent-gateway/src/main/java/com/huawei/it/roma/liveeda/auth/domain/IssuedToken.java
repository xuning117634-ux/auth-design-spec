package com.huawei.it.roma.liveeda.auth.domain;

import java.time.Instant;

public record IssuedToken(
        String accessToken,
        Instant expiresAt
) {
    public long expiresInSeconds(Instant now) {
        return Math.max(0, expiresAt.getEpochSecond() - now.getEpochSecond());
    }
}

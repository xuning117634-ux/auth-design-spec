package com.huawei.it.roma.liveeda.auth.domain;

import java.time.Instant;

public record ResourceCookieEntry(
        String agentId,
        String trHash,
        String cookie,
        Instant expiresAt,
        Instant createdAt
) {
}

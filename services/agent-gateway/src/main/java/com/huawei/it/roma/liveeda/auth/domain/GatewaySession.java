package com.huawei.it.roma.liveeda.auth.domain;

import java.time.Instant;

public record GatewaySession(
        String gatewaySessionId,
        String gatewaySessionToken,
        String userId,
        String username,
        Instant createdAt
) {
}

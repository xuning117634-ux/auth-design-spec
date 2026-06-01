package com.huawei.it.roma.liveeda.auth.domain;

import java.time.Instant;
import java.util.List;

public record TokenResultTicket(
        String tokenResultTicket,
        String requestId,
        String agentId,
        String trToken,
        String userId,
        String username,
        List<AuthorizedPermissionPoint> consentedScopes,
        Instant expiresAt,
        Instant createdAt
) {
}

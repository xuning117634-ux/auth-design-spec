package com.huawei.it.roma.liveeda.auth.domain;

import java.time.Instant;
import java.util.Set;

public record UserAuthorizationResult(
        String userId,
        String username,
        Set<String> consentedPolicyCodes,
        String accessToken,
        Instant expiresAt
) {
}

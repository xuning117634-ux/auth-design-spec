package com.huawei.it.roma.liveeda.auth.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;

public record GatewayAuthContext(
        String gatewaySessionId,
        String agentId,
        String tcToken,
        String t1Token,
        String trToken,
        Set<String> consentedPolicyCodes,
        Instant expiresAt
) {
    public boolean covers(Set<String> requiredPolicyCodes, Clock clock) {
        return !isExpired(clock) && consentedPolicyCodes.containsAll(requiredPolicyCodes);
    }

    public boolean isExpired(Clock clock) {
        return expiresAt != null && expiresAt.isBefore(clock.instant());
    }
}

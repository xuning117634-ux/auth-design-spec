package com.huawei.it.roma.liveeda.auth.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public record GatewayAuthContext(
        String gatewaySessionId,
        String agentId,
        String tcToken,
        String t1Token,
        String trToken,
        List<AuthorizedPermissionPoint> authorizedPermissionPoints,
        Instant expiresAt
) {
    public boolean covers(Set<String> requiredPermissionPointCodes, Clock clock) {
        Set<String> grantedCodes = authorizedPermissionPoints == null ? Set.of() : authorizedPermissionPoints.stream()
                .map(AuthorizedPermissionPoint::code)
                .collect(java.util.stream.Collectors.toSet());
        return !isExpired(clock) && grantedCodes.containsAll(requiredPermissionPointCodes);
    }

    public boolean isExpired(Clock clock) {
        return expiresAt != null && expiresAt.isBefore(clock.instant());
    }
}

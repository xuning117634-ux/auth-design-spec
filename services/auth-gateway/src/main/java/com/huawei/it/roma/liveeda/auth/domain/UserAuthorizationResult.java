package com.huawei.it.roma.liveeda.auth.domain;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public record UserAuthorizationResult(
        String userId,
        String username,
        Set<String> authorizedPermissionPointCodes,
        List<AuthorizedPermissionPoint> authorizedPermissionPoints,
        String accessToken,
        Instant expiresAt
) {
}

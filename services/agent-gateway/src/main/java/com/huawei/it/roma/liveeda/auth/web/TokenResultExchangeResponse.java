package com.huawei.it.roma.liveeda.auth.web;

import com.huawei.it.roma.liveeda.auth.domain.AuthorizedPermissionPoint;
import java.util.List;

public record TokenResultExchangeResponse(
        String status,
        String requestId,
        String accessToken,
        Long expiresIn,
        AgencyUser agencyUser,
        List<AuthorizedPermissionPoint> consentedScopes
) {
    public record AgencyUser(
            String userId,
            String globalUserId,
            String username
    ) {
    }
}

package com.huawei.it.roma.liveeda.demoagent.client;

import java.util.List;

public record GatewayTokenResponse(
        String accessToken,
        Long expiresIn,
        String status,
        String redirectUrl,
        String requestId,
        AgencyUser agencyUser,
        List<ConsentedScope> consentedScopes
) {
    public GatewayTokenResponse(String accessToken, Long expiresIn, String status, String redirectUrl, String requestId) {
        this(accessToken, expiresIn, status, redirectUrl, requestId, null, List.of());
    }

    public boolean isRedirect() {
        return ("redirect".equalsIgnoreCase(status) || "REDIRECT_REQUIRED".equalsIgnoreCase(status)) && redirectUrl != null;
    }

    public boolean isTokenReady() {
        return accessToken != null && !accessToken.isBlank();
    }

    public record AgencyUser(
            String userId,
            String globalUserId,
            String username
    ) {
        public AgencyUser(String userId, String globalUserId) {
            this(userId, globalUserId, null);
        }
    }

    public record ConsentedScope(
            String code,
            String displayNameZh
    ) {
    }
}

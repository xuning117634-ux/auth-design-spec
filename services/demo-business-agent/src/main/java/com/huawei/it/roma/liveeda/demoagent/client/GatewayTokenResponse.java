package com.huawei.it.roma.liveeda.demoagent.client;

public record GatewayTokenResponse(
        String accessToken,
        Long expiresIn,
        String status,
        String redirectUrl,
        String requestId
) {
    public boolean isRedirect() {
        return "redirect".equalsIgnoreCase(status) && redirectUrl != null;
    }

    public boolean isTokenReady() {
        return accessToken != null && !accessToken.isBlank();
    }
}

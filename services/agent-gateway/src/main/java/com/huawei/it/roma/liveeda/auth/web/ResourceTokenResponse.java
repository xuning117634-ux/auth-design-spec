package com.huawei.it.roma.liveeda.auth.web;

public record ResourceTokenResponse(
        String accessToken,
        Long expiresIn,
        String status,
        String redirectUrl,
        String requestId
) {
    public static ResourceTokenResponse direct(String accessToken, long expiresIn) {
        return new ResourceTokenResponse(accessToken, expiresIn, null, null, null);
    }

    public static ResourceTokenResponse redirect(String redirectUrl, String requestId) {
        return new ResourceTokenResponse(null, null, "redirect", redirectUrl, requestId);
    }
}

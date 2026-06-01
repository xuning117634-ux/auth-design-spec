package com.huawei.it.roma.liveeda.auth.web;

public record LoginTicketExchangeResponse(
        UserInfo user,
        long expiresIn
) {
    public record UserInfo(
            String userId,
            String uuid,
            String username
    ) {
    }
}

package com.huawei.it.roma.liveeda.demoagent.client;

public record LoginTicketExchangeResponse(
        UserInfo user,
        Long expiresIn
) {
    public record UserInfo(
            String userId,
            String uuid,
            String username
    ) {
    }
}

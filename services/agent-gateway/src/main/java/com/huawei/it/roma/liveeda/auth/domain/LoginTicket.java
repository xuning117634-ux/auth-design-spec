package com.huawei.it.roma.liveeda.auth.domain;

import java.net.URI;
import java.time.Instant;

public record LoginTicket(
        String ticketST,
        String agentId,
        String authorizationCode,
        String clientId,
        String redirectUri,
        URI returnUrl,
        Instant createdAt
) {
}

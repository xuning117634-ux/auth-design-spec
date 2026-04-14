package com.huawei.it.roma.liveeda.demoagent.domain;

import java.time.Instant;

public record SiteSession(
        String siteSessionId,
        String gwSessionToken,
        String userId,
        String username,
        Instant createdAt
) {
}

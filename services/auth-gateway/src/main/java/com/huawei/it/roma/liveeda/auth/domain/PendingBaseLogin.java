package com.huawei.it.roma.liveeda.auth.domain;

import java.net.URI;

public record PendingBaseLogin(
        String gwState,
        String agentId,
        URI returnUrl,
        String outerState
) {
}

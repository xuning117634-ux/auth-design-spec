package com.huawei.it.roma.liveeda.auth.domain;

import java.net.URI;
import java.util.Set;

public record PendingAuthTransaction(
        String requestId,
        String agentId,
        Set<String> requiredTools,
        Set<String> requiredPolicyCodes,
        URI returnUrl,
        String outerState,
        String gatewaySessionId,
        String gwState
) {
    public PendingAuthTransaction withGwState(String updatedGwState) {
        return new PendingAuthTransaction(
                requestId,
                agentId,
                requiredTools,
                requiredPolicyCodes,
                returnUrl,
                outerState,
                gatewaySessionId,
                updatedGwState
        );
    }
}

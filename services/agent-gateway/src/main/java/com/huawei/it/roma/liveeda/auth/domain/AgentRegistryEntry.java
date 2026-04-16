package com.huawei.it.roma.liveeda.auth.domain;

import java.util.List;

public record AgentRegistryEntry(
        String agentId,
        String agentName,
        String appId,
        String agentServiceAccount,
        List<String> allowedReturnHosts,
        String status
) {
    public boolean isActive() {
        return status != null && "ACTIVE".equalsIgnoreCase(status);
    }
}

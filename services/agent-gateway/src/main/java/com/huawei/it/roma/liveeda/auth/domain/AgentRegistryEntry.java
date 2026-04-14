package com.huawei.it.roma.liveeda.auth.domain;

import java.util.List;
import java.util.Set;

public record AgentRegistryEntry(
        String agentId,
        String agentName,
        String agentServiceAccount,
        String principal,
        Set<String> subscribedTools,
        List<String> allowedReturnHosts
) {
}

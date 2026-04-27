package com.huawei.it.roma.liveeda.auth.domain;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record AgentRegistryEntry(
        String agentId,
        String agentName,
        String enterprise,
        String appId,
        List<String> allowedReturnHosts,
        Set<String> subscribedPermissionPointCodes
) {
    public boolean hasSubscribedAll(Set<String> requiredPermissionPointCodes) {
        return new LinkedHashSet<>(subscribedPermissionPointCodes).containsAll(requiredPermissionPointCodes);
    }
}

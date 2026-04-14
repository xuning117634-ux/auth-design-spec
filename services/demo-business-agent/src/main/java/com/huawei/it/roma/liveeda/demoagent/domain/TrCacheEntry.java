package com.huawei.it.roma.liveeda.demoagent.domain;

import java.time.Instant;
import java.util.Set;

public record TrCacheEntry(
        String siteSessionId,
        String agentId,
        String currentTr,
        Set<String> coveredTools,
        Set<String> coveredPolicyCodes,
        Instant expiresAt
) {
    public boolean covers(Set<String> requiredTools, Instant now) {
        return expiresAt.isAfter(now) && coveredTools.containsAll(requiredTools);
    }
}

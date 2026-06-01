package com.huawei.it.roma.liveeda.auth.domain;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record PendingAuthTransaction(
        String requestId,
        String agentId,
        Set<String> requiredTools,
        Set<String> requiredPermissionPointCodes,
        List<AuthorizedPermissionPoint> requiredPermissionPoints,
        URI returnUrl,
        String outerState,
        Map<String, String> subjectHint,
        String gwState
) {
    public PendingAuthTransaction withGwState(String updatedGwState) {
        return new PendingAuthTransaction(
                requestId,
                agentId,
                requiredTools,
                requiredPermissionPointCodes,
                requiredPermissionPoints,
                returnUrl,
                outerState,
                subjectHint,
                updatedGwState
        );
    }
}

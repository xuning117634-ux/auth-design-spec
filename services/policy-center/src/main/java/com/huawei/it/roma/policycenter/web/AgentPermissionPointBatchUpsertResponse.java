package com.huawei.it.roma.policycenter.web;

import java.util.List;

public record AgentPermissionPointBatchUpsertResponse(
        String agentId,
        String enterprise,
        int upsertedCount,
        List<ItemResult> items
) {
    public record ItemResult(
            String permissionPointCode,
            String result
    ) {
    }
}

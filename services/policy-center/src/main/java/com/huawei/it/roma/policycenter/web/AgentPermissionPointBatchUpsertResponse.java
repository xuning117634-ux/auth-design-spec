package com.huawei.it.roma.policycenter.web;

import java.util.List;

public record AgentPermissionPointBatchUpsertResponse(
        String agentId,
        String enterprise,
        int permissionPointCount,
        List<String> permissionPointCodes
) {
}

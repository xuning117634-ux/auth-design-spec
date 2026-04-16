package com.huawei.it.roma.policycenter.web;

import com.huawei.it.roma.policycenter.domain.PermissionPointItem;
import java.util.List;

public record ResolveByToolsResponse(
        String agentId,
        List<String> requiredPermissionPointCodes,
        List<PermissionPointItem> permissionPoints
) {
}

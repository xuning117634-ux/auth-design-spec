package com.huawei.it.roma.policycenter.web;

import com.huawei.it.roma.policycenter.domain.PermissionPointItem;
import com.huawei.it.roma.policycenter.domain.ToolItem;
import java.util.List;

public record ResolveByCodesResponse(
        List<String> permissionPointCodes,
        List<String> allowedTools,
        List<PermissionPointItem> permissionPoints,
        List<ToolItem> toolItems
) {
}

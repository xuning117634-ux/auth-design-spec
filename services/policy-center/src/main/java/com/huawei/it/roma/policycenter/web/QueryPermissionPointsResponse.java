package com.huawei.it.roma.policycenter.web;

import com.huawei.it.roma.policycenter.domain.ToolItem;
import java.util.List;

public record QueryPermissionPointsResponse(
        List<PermissionPoint> permissionPoints
) {
    public record PermissionPoint(
            String permissionPointCode,
            String enterprise,
            String appId,
            String displayNameZh,
            String description,
            List<ToolItem> boundTools,
            String status
    ) {
    }
}

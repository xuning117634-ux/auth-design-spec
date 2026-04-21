package com.huawei.it.roma.policycenter.web;

import java.util.List;

public record PermissionPointBatchUpsertResponse(
        int upsertedCount,
        List<ItemResult> items
) {
    public record ItemResult(
            String permissionPointCode,
            String result
    ) {
    }
}

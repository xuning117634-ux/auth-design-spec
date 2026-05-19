package com.huawei.it.roma.policycenter.web;

import java.util.List;

public record BatchHardDeleteResponse(
        int deletedCount,
        List<ItemResult> items
) {
    public record ItemResult(
            String id,
            String result
    ) {
    }
}

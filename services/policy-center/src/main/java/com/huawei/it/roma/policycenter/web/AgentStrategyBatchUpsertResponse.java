package com.huawei.it.roma.policycenter.web;

import java.util.List;

public record AgentStrategyBatchUpsertResponse(
        String agentId,
        int upsertedCount,
        List<ItemResult> items
) {
    public record ItemResult(
            String strategyId,
            String result
    ) {
    }
}

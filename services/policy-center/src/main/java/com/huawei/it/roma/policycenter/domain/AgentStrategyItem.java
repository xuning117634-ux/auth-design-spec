package com.huawei.it.roma.policycenter.domain;

public record AgentStrategyItem(
        String strategyId,
        String agentId,
        String permissionPointCode,
        StrategyCondition conditions,
        String effect,
        String status
) {
}

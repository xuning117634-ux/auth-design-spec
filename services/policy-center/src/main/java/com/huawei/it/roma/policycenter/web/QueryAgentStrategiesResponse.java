package com.huawei.it.roma.policycenter.web;

import com.huawei.it.roma.policycenter.domain.AgentStrategyItem;
import java.util.List;

public record QueryAgentStrategiesResponse(
        String agentId,
        List<String> permissionPointCodes,
        List<AgentStrategyItem> strategies
) {
}

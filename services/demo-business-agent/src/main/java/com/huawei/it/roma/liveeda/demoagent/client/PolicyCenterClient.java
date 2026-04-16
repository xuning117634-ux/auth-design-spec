package com.huawei.it.roma.liveeda.demoagent.client;

import com.huawei.it.roma.liveeda.demoagent.config.DemoAgentProperties;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class PolicyCenterClient {

    private final RestClient.Builder restClientBuilder;
    private final DemoAgentProperties properties;

    public ResolveByToolsResult resolveByTools(String agentId, Collection<String> requiredTools) {
        RestClient restClient = restClientBuilder.baseUrl(properties.getPolicyCenterBaseUrl()).build();
        return restClient.post()
                .uri("/internal/v1/permission-points/resolve-by-tools")
                .body(new ResolveByToolsRequest(agentId, requiredTools.stream().sorted().toList()))
                .retrieve()
                .body(ResolveByToolsResult.class);
    }

    public QueryAgentStrategiesResult queryAgentStrategies(String agentId, Collection<String> permissionPointCodes) {
        RestClient restClient = restClientBuilder.baseUrl(properties.getPolicyCenterBaseUrl()).build();
        return restClient.post()
                .uri("/internal/v1/agent-strategies/query")
                .body(new QueryAgentStrategiesRequest(agentId, permissionPointCodes.stream().sorted().toList()))
                .retrieve()
                .body(QueryAgentStrategiesResult.class);
    }

    private record ResolveByToolsRequest(String agentId, List<String> requiredTools) {
    }

    private record QueryAgentStrategiesRequest(String agentId, List<String> permissionPointCodes) {
    }

    public record ResolveByToolsResult(
            String agentId,
            List<String> requiredPermissionPointCodes,
            List<PermissionPointView> permissionPoints
    ) {
    }

    public record QueryAgentStrategiesResult(
            String agentId,
            List<String> permissionPointCodes,
            List<AgentStrategyView> strategies
    ) {
    }

    public record PermissionPointView(
            String code,
            String displayNameZh
    ) {
    }

    public record AgentStrategyView(
            String strategyId,
            String agentId,
            String permissionPointCode,
            StrategyConditionView conditions,
            String effect,
            String status
    ) {
    }

    public record StrategyConditionView(
            String field,
            String operator,
            List<String> values
    ) {
    }
}

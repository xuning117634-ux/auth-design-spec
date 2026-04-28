package com.huawei.it.roma.liveeda.demoagent.client;

import com.huawei.it.roma.liveeda.demoagent.config.DemoAgentProperties;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestBodySpec;

@Component
public class PolicyCenterClient {

    private final RestClient.Builder restClientBuilder;
    private final DemoAgentProperties properties;

    public PolicyCenterClient(RestClient.Builder restClientBuilder, DemoAgentProperties properties) {
        this.restClientBuilder = restClientBuilder;
        this.properties = properties;
    }

    public ResolveByToolsResult resolveByTools(Collection<String> requiredTools) {
        RestClient restClient = restClientBuilder.baseUrl(properties.getPolicyCenterBaseUrl()).build();
        return post(restClient, "/internal/v1/permission-points/resolve-by-tools")
                .body(new ResolveByToolsRequest(requiredTools.stream().sorted().toList()))
                .retrieve()
                .body(ResolveByToolsResult.class);
    }

    public ResolveByCodesResult resolveByCodes(Collection<String> permissionPointCodes) {
        RestClient restClient = restClientBuilder.baseUrl(properties.getPolicyCenterBaseUrl()).build();
        return post(restClient, "/internal/v1/permission-points/resolve-by-codes")
                .body(new ResolveByCodesRequest(permissionPointCodes.stream().sorted().toList()))
                .retrieve()
                .body(ResolveByCodesResult.class);
    }

    public QueryAgentStrategiesResult queryAgentStrategies(String agentId, Collection<String> permissionPointCodes) {
        RestClient restClient = restClientBuilder.baseUrl(properties.getPolicyCenterBaseUrl()).build();
        return post(restClient, "/internal/v1/agent-strategies/query")
                .body(new QueryAgentStrategiesRequest(agentId, permissionPointCodes.stream().sorted().toList()))
                .retrieve()
                .body(QueryAgentStrategiesResult.class);
    }

    private RequestBodySpec post(RestClient restClient, String uri) {
        RequestBodySpec request = restClient.post().uri(uri);
        for (Map.Entry<String, String> entry : properties.getPolicyCenterHeaders().entrySet()) {
            if (!isBlank(entry.getKey()) && !isBlank(entry.getValue())) {
                request.header(entry.getKey(), entry.getValue());
            }
        }
        return request;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record ResolveByToolsRequest(List<String> requiredTools) {
    }

    private record ResolveByCodesRequest(List<String> permissionPointCodes) {
    }

    private record QueryAgentStrategiesRequest(String agentId, List<String> permissionPointCodes) {
    }

    public record ResolveByToolsResult(
            List<String> requiredPermissionPointCodes,
            List<PermissionPointView> permissionPoints
    ) {
    }

    public record ResolveByCodesResult(
            List<String> permissionPointCodes,
            List<PermissionPointView> permissionPoints,
            List<ToolView> tools
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

    public record ToolView(
            String toolId,
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

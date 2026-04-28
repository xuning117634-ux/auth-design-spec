package com.huawei.it.roma.liveeda.auth.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.huawei.it.roma.liveeda.auth.config.AgentManagementClientProperties;
import com.huawei.it.roma.liveeda.auth.domain.AgentRegistryEntry;
import com.huawei.it.roma.liveeda.auth.web.GatewayException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient.RequestHeadersSpec;
import org.springframework.web.client.RestClient;

@Component
@Profile("real")
@RequiredArgsConstructor
public class RealAgentManagementClient implements AgentManagementClient {

    private final RestClient.Builder restClientBuilder;
    private final AgentManagementClientProperties properties;

    @Override
    public AgentRegistryEntry getGatewayProfile(String agentId) {
        RestClient restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
        RequestHeadersSpec<?> request = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(properties.getQueryByAgentIdPath())
                        .queryParam("agentId", agentId)
                        .build());
        applyConfiguredHeaders(request);
        AgentMallResponse response = request
                .retrieve()
                .body(AgentMallResponse.class);
        if (response == null || !"0000".equals(response.status()) || response.data() == null) {
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "Agent management returned empty response");
        }
        AgentMallData data = response.data();
        validate(data, agentId);
        return new AgentRegistryEntry(
                data.uniqueId(),
                data.name(),
                data.enterpriseId(),
                data.appId(),
                data.allowedReturnHosts(),
                new LinkedHashSet<>(data.subscriptionPermissionPoints())
        );
    }

    private void applyConfiguredHeaders(RequestHeadersSpec<?> request) {
        for (Map.Entry<String, String> entry : properties.getHeaders().entrySet()) {
            if (!isBlank(entry.getKey()) && !isBlank(entry.getValue())) {
                request.header(entry.getKey(), entry.getValue());
            }
        }
    }

    private void validate(AgentMallData data, String expectedAgentId) {
        if (!expectedAgentId.equals(data.uniqueId())) {
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "Agent management returned mismatched agentId");
        }
        if (isBlank(data.name()) || isBlank(data.enterpriseId()) || isBlank(data.appId())) {
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "Agent management response is missing required fields");
        }
        if (data.allowedReturnHosts() == null || data.allowedReturnHosts().isEmpty()) {
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "Agent management response is missing allowedReturnHosts");
        }
        if (data.subscriptionPermissionPoints() == null) {
            throw new GatewayException(HttpStatus.BAD_GATEWAY,
                    "Agent management response is missing subscriptionPermissionPoints");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record AgentMallResponse(
            String status,
            String message,
            AgentMallData data
    ) {
    }

    private record AgentMallData(
            @JsonProperty("uniqueId") String uniqueId,
            @JsonProperty("name") String name,
            @JsonProperty("enterpriseId") String enterpriseId,
            @JsonProperty("appId") String appId,
            @JsonProperty("allowedReturnHosts") List<String> allowedReturnHosts,
            @JsonProperty("subscriptionPermissionPoints") Set<String> subscriptionPermissionPoints
    ) {
    }
}

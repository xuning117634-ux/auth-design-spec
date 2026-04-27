package com.huawei.it.roma.liveeda.auth.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.huawei.it.roma.liveeda.auth.config.AgentManagementClientProperties;
import com.huawei.it.roma.liveeda.auth.domain.AgentRegistryEntry;
import com.huawei.it.roma.liveeda.auth.web.GatewayException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
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
        GatewayProfileResponse response = restClient.get()
                .uri("/internal/v1/agents/{agentId}/gateway-profile", agentId)
                .retrieve()
                .body(GatewayProfileResponse.class);
        if (response == null) {
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "Agent management returned empty response");
        }
        validate(response, agentId);
        return new AgentRegistryEntry(
                response.agentId(),
                response.agentName(),
                response.enterprise(),
                response.appId(),
                response.allowedReturnHosts(),
                new LinkedHashSet<>(response.subscribedPermissionPointCodes())
        );
    }

    private void validate(GatewayProfileResponse response, String expectedAgentId) {
        if (!expectedAgentId.equals(response.agentId())) {
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "Agent management returned mismatched agentId");
        }
        if (isBlank(response.agentName()) || isBlank(response.enterprise()) || isBlank(response.appId())) {
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "Agent management response is missing required fields");
        }
        if (response.allowedReturnHosts() == null || response.allowedReturnHosts().isEmpty()) {
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "Agent management response is missing allowedReturnHosts");
        }
        if (response.subscribedPermissionPointCodes() == null) {
            throw new GatewayException(HttpStatus.BAD_GATEWAY,
                    "Agent management response is missing subscribedPermissionPointCodes");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record GatewayProfileResponse(
            @JsonProperty("agentId") String agentId,
            @JsonProperty("agentName") String agentName,
            @JsonProperty("enterprise") String enterprise,
            @JsonProperty("appId") String appId,
            @JsonProperty("allowedReturnHosts") List<String> allowedReturnHosts,
            @JsonProperty("subscribedPermissionPointCodes") Set<String> subscribedPermissionPointCodes
    ) {
    }
}

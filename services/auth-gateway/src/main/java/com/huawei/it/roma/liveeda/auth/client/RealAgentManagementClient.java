package com.huawei.it.roma.liveeda.auth.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.huawei.it.roma.liveeda.auth.config.AgentManagementClientProperties;
import com.huawei.it.roma.liveeda.auth.domain.AgentRegistryEntry;
import com.huawei.it.roma.liveeda.auth.util.LogSanitizer;
import com.huawei.it.roma.liveeda.auth.web.GatewayException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient.RequestHeadersSpec;
import org.springframework.web.client.RestClient;

@Component
@Profile("real")
@RequiredArgsConstructor
@Slf4j
public class RealAgentManagementClient implements AgentManagementClient {

    private final RestClient.Builder restClientBuilder;
    private final AgentManagementClientProperties properties;

    @Override
    public AgentRegistryEntry getGatewayProfile(String agentId) {
        RestClient restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
        long startNanos = System.nanoTime();
        log.info("agent management request started, operation=getGatewayProfile, baseUrl={}, agentId={}",
                properties.getBaseUrl(), agentId);
        RequestHeadersSpec<?> request = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(properties.getQueryByAgentIdPath())
                        .queryParam("agentId", agentId)
                        .build());
        applyConfiguredHeaders(request);
        AgentMallResponse response;
        try {
            response = request
                    .retrieve()
                    .body(AgentMallResponse.class);
        } catch (RuntimeException exception) {
            log.error("agent management request failed, operation=getGatewayProfile, agentId={}, elapsedMs={}, error={}",
                    agentId, LogSanitizer.elapsedMillis(startNanos), exception.getClass().getSimpleName(), exception);
            throw exception;
        }
        if (response == null || !"0000".equals(response.status()) || response.data() == null) {
            log.warn("agent management returned invalid response, operation=getGatewayProfile, agentId={}, status={}, elapsedMs={}",
                    agentId, response == null ? null : response.status(), LogSanitizer.elapsedMillis(startNanos));
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "Agent management returned empty response");
        }
        AgentMallData data = response.data();
        validate(data, agentId);
        log.info("agent management request completed, operation=getGatewayProfile, agentId={}, appId={}, permissionPoints={}, elapsedMs={}",
                data.uniqueId(), data.appId(), LogSanitizer.size(data.subscriptionPermissionPoints()),
                LogSanitizer.elapsedMillis(startNanos));
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

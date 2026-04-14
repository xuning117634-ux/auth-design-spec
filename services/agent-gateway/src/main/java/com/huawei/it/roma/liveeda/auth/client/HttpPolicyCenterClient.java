package com.huawei.it.roma.liveeda.auth.client;

import com.huawei.it.roma.liveeda.auth.config.PolicyCenterClientProperties;
import com.huawei.it.roma.liveeda.auth.web.GatewayException;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class HttpPolicyCenterClient implements PolicyCenterClient {

    private final RestClient.Builder restClientBuilder;
    private final PolicyCenterClientProperties properties;

    @Override
    public PolicyResolutionResult resolveByTools(String agentId, Set<String> requiredTools) {
        RestClient restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
        ResolveByToolsClientResponse response = restClient.post()
                .uri("/internal/v1/policies/resolve-by-tools")
                .body(new ResolveByToolsClientRequest(agentId, requiredTools.stream().sorted().toList()))
                .retrieve()
                .body(ResolveByToolsClientResponse.class);
        if (response == null || response.requiredPolicyCodes() == null || response.requiredPolicyCodes().isEmpty()) {
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "Policy center returned empty policy mapping");
        }
        return new PolicyResolutionResult(Set.copyOf(response.requiredPolicyCodes()));
    }

    private record ResolveByToolsClientRequest(String agentId, List<String> requiredTools) {
    }

    private record ResolveByToolsClientResponse(List<String> requiredPolicyCodes) {
    }
}

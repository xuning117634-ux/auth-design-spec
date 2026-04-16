package com.huawei.it.roma.liveeda.auth.client;

import com.huawei.it.roma.liveeda.auth.domain.AuthorizedPermissionPoint;
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
                .uri("/internal/v1/permission-points/resolve-by-tools")
                .body(new ResolveByToolsClientRequest(agentId, requiredTools.stream().sorted().toList()))
                .retrieve()
                .body(ResolveByToolsClientResponse.class);
        if (response == null || response.requiredPermissionPointCodes() == null
                || response.requiredPermissionPointCodes().isEmpty()) {
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "Policy center returned empty permission point mapping");
        }
        return new PolicyResolutionResult(
                Set.copyOf(response.requiredPermissionPointCodes()),
                response.permissionPoints() == null ? List.of() : response.permissionPoints()
        );
    }

    @Override
    public List<AuthorizedPermissionPoint> resolveByCodes(Set<String> permissionPointCodes) {
        RestClient restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
        ResolveByCodesClientResponse response = restClient.post()
                .uri("/internal/v1/permission-points/resolve-by-codes")
                .body(new ResolveByCodesClientRequest(permissionPointCodes.stream().sorted().toList()))
                .retrieve()
                .body(ResolveByCodesClientResponse.class);
        if (response == null || response.permissionPoints() == null || response.permissionPoints().isEmpty()) {
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "Policy center returned empty permission point catalog");
        }
        return response.permissionPoints();
    }

    private record ResolveByToolsClientRequest(String agentId, List<String> requiredTools) {
    }

    private record ResolveByToolsClientResponse(
            List<String> requiredPermissionPointCodes,
            List<AuthorizedPermissionPoint> permissionPoints
    ) {
    }

    private record ResolveByCodesClientRequest(List<String> permissionPointCodes) {
    }

    private record ResolveByCodesClientResponse(
            List<String> permissionPointCodes,
            List<String> allowedTools,
            List<AuthorizedPermissionPoint> permissionPoints
    ) {
    }
}

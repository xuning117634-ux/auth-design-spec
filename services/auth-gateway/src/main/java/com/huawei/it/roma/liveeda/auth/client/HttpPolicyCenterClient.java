package com.huawei.it.roma.liveeda.auth.client;

import com.huawei.it.roma.liveeda.auth.config.PolicyCenterClientProperties;
import com.huawei.it.roma.liveeda.auth.domain.AuthorizedPermissionPoint;
import com.huawei.it.roma.liveeda.auth.util.LogSanitizer;
import com.huawei.it.roma.liveeda.auth.web.GatewayException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestBodySpec;

@Component
@Slf4j
public class HttpPolicyCenterClient implements PolicyCenterClient {

    private final RestClient.Builder restClientBuilder;
    private final PolicyCenterClientProperties properties;

    public HttpPolicyCenterClient(RestClient.Builder restClientBuilder, PolicyCenterClientProperties properties) {
        this.restClientBuilder = restClientBuilder;
        this.properties = properties;
    }

    @Override
    public PolicyResolutionResult resolveByTools(Set<String> requiredTools) {
        RestClient restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
        long startNanos = System.nanoTime();
        log.info("policy center request started, operation=resolveByTools, baseUrl={}, requiredTools={}",
                properties.getBaseUrl(), LogSanitizer.size(requiredTools));
        ResolveByToolsClientResponse response;
        try {
            response = post(restClient, "/internal/v1/permission-points/resolve-by-tools")
                    .body(new ResolveByToolsClientRequest(requiredTools.stream().sorted().toList()))
                    .retrieve()
                    .body(ResolveByToolsClientResponse.class);
        } catch (RuntimeException exception) {
            log.error("policy center request failed, operation=resolveByTools, requiredTools={}, elapsedMs={}, error={}",
                    LogSanitizer.size(requiredTools), LogSanitizer.elapsedMillis(startNanos),
                    exception.getClass().getSimpleName(), exception);
            throw exception;
        }
        if (response == null || response.requiredPermissionPointCodes() == null
                || response.requiredPermissionPointCodes().isEmpty()) {
            log.warn("policy center returned empty mapping, operation=resolveByTools, requiredTools={}, elapsedMs={}",
                    LogSanitizer.size(requiredTools), LogSanitizer.elapsedMillis(startNanos));
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "Policy center returned empty permission point mapping");
        }
        log.info("policy center request completed, operation=resolveByTools, requiredTools={}, permissionPoints={}, elapsedMs={}",
                LogSanitizer.size(requiredTools), LogSanitizer.size(response.requiredPermissionPointCodes()),
                LogSanitizer.elapsedMillis(startNanos));
        return new PolicyResolutionResult(
                Set.copyOf(response.requiredPermissionPointCodes()),
                response.permissionPoints() == null ? List.of() : response.permissionPoints()
        );
    }

    @Override
    public List<AuthorizedPermissionPoint> resolveByCodes(Set<String> permissionPointCodes) {
        RestClient restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
        long startNanos = System.nanoTime();
        log.info("policy center request started, operation=resolveByCodes, baseUrl={}, permissionPointCodes={}",
                properties.getBaseUrl(), LogSanitizer.size(permissionPointCodes));
        ResolveByCodesClientResponse response;
        try {
            response = post(restClient, "/internal/v1/permission-points/resolve-by-codes")
                    .body(new ResolveByCodesClientRequest(permissionPointCodes.stream().sorted().toList()))
                    .retrieve()
                    .body(ResolveByCodesClientResponse.class);
        } catch (RuntimeException exception) {
            log.error("policy center request failed, operation=resolveByCodes, permissionPointCodes={}, elapsedMs={}, error={}",
                    LogSanitizer.size(permissionPointCodes), LogSanitizer.elapsedMillis(startNanos),
                    exception.getClass().getSimpleName(), exception);
            throw exception;
        }
        if (response == null || response.permissionPoints() == null || response.permissionPoints().isEmpty()) {
            log.warn("policy center returned empty catalog, operation=resolveByCodes, permissionPointCodes={}, elapsedMs={}",
                    LogSanitizer.size(permissionPointCodes), LogSanitizer.elapsedMillis(startNanos));
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "Policy center returned empty permission point catalog");
        }
        log.info("policy center request completed, operation=resolveByCodes, permissionPointCodes={}, permissionPoints={}, elapsedMs={}",
                LogSanitizer.size(permissionPointCodes), LogSanitizer.size(response.permissionPoints()),
                LogSanitizer.elapsedMillis(startNanos));
        return response.permissionPoints();
    }

    private RequestBodySpec post(RestClient restClient, String uri) {
        RequestBodySpec request = restClient.post().uri(uri);
        for (Map.Entry<String, String> entry : properties.getHeaders().entrySet()) {
            if (!isBlank(entry.getKey()) && !isBlank(entry.getValue())) {
                request.header(entry.getKey(), entry.getValue());
            }
        }
        return request;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record ResolveByToolsClientRequest(List<String> requiredTools) {
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
            List<AuthorizedPermissionPoint> permissionPoints
    ) {
    }
}

package com.huawei.it.roma.liveeda.demoagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.it.roma.liveeda.demoagent.client.PolicyCenterClient;
import com.huawei.it.roma.liveeda.demoagent.client.PolicyCenterClient.ResolveByCodesResult;
import com.huawei.it.roma.liveeda.demoagent.config.DemoAgentProperties;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestBodySpec;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "demo-agent.mcp", name = "mode", havingValue = "real")
public class RealMcpGatewayClient implements McpGatewayClient {

    private final RestClient.Builder restClientBuilder;
    private final DemoAgentProperties properties;
    private final ObjectMapper objectMapper;
    private final PolicyCenterClient policyCenterClient;

    @Override
    public Set<String> extractAuthorizedPermissionPointCodes(String trToken) {
        return new LinkedHashSet<>(decodeTrToken(trToken).authorizedPermissionPointCodes());
    }

    @Override
    public Set<String> resolveCoveredTools(String trToken) {
        DecodedTrContext trContext = decodeTrToken(trToken);
        ResolveByCodesResult resolution = policyCenterClient.resolveByCodes(trContext.authorizedPermissionPointCodes());
        if (resolution == null || resolution.tools() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "策略中心未返回 TR 对应的工具集合");
        }
        return resolution.tools().stream()
                .map(PolicyCenterClient.ToolView::toolId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public String invoke(String agentId, String trToken, Set<String> requiredTools, String message) {
        RestClient restClient = restClientBuilder.baseUrl(properties.getMcp().getGatewayBaseUrl()).build();
        InvokeResponse response = post(restClient)
                .body(new InvokeRequest(agentId, trToken, requiredTools.stream().sorted().toList(), message))
                .retrieve()
                .body(InvokeResponse.class);
        if (response == null || !"SUCCESS".equalsIgnoreCase(response.status()) || isBlank(response.answer())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "真实 MCP 网关未返回可用结果");
        }
        return response.answer();
    }

    private RequestBodySpec post(RestClient restClient) {
        RequestBodySpec request = restClient.post().uri(properties.getMcp().getInvokePath());
        for (Map.Entry<String, String> entry : properties.getMcp().getHeaders().entrySet()) {
            if (!isBlank(entry.getKey()) && !isBlank(entry.getValue())) {
                request.header(entry.getKey(), entry.getValue());
            }
        }
        return request;
    }

    private DecodedTrContext decodeTrToken(String trToken) {
        try {
            String[] parts = trToken.split("\\.");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid JWT format");
            }
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode payload = objectMapper.readTree(new String(payloadBytes, StandardCharsets.UTF_8));
            JsonNode consentedScopes = payload.path("agency_user").path("consented_scopes");
            Set<String> permissionPointCodes = new LinkedHashSet<>();
            if (consentedScopes.isArray()) {
                consentedScopes.forEach(node -> {
                    String code = node.path("code").asText();
                    if (!code.isBlank()) {
                        permissionPointCodes.add(code);
                    }
                });
            }
            return new DecodedTrContext(permissionPointCodes);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "当前资源令牌不可解析，无法调用真实 MCP 网关", exception);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record InvokeRequest(
            String agentId,
            String tr,
            List<String> tools,
            String message
    ) {
    }

    private record InvokeResponse(
            String status,
            String answer
    ) {
    }

    private record DecodedTrContext(Set<String> authorizedPermissionPointCodes) {
    }
}

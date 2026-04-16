package com.huawei.it.roma.liveeda.demoagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.it.roma.liveeda.demoagent.client.PolicyCenterClient;
import com.huawei.it.roma.liveeda.demoagent.client.PolicyCenterClient.AgentStrategyView;
import com.huawei.it.roma.liveeda.demoagent.client.PolicyCenterClient.PermissionPointView;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class MockMcpGatewayClient {

    private final PolicyCenterClient policyCenterClient;
    private final MockMcpClient mockMcpClient;
    private final ObjectMapper objectMapper;

    public Set<String> extractAuthorizedPermissionPointCodes(String trToken) {
        return new LinkedHashSet<>(decodeTrToken(trToken).authorizedPermissionPointCodes());
    }

    public String invoke(String agentId, String trToken, Set<String> requiredTools, String message) {
        DecodedTrContext trContext = decodeTrToken(trToken);
        PolicyCenterClient.ResolveByToolsResult toolResolution = policyCenterClient.resolveByTools(agentId, requiredTools);
        Set<String> trAuthorizedCodes = new LinkedHashSet<>(trContext.authorizedPermissionPointCodes());

        Map<String, String> permissionPointLabels = new LinkedHashMap<>();
        for (PermissionPointView permissionPoint : toolResolution.permissionPoints()) {
            permissionPointLabels.put(permissionPoint.code(), permissionPoint.displayNameZh());
        }

        for (String requiredCode : toolResolution.requiredPermissionPointCodes()) {
            if (!trAuthorizedCodes.contains(requiredCode)) {
                throw forbidden("当前请求缺少所需的用户授权：" + permissionPointLabels.getOrDefault(requiredCode, requiredCode));
            }
        }

        Map<String, List<AgentStrategyView>> strategiesByCode = policyCenterClient
                .queryAgentStrategies(agentId, toolResolution.requiredPermissionPointCodes())
                .strategies()
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        AgentStrategyView::permissionPointCode,
                        LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ));

        for (String requiredCode : toolResolution.requiredPermissionPointCodes()) {
            List<AgentStrategyView> strategies = strategiesByCode.getOrDefault(requiredCode, List.of());
            if (!isPermissionPointAllowed(strategies, trContext.userId())) {
                throw forbidden("当前用户无权使用该 Agent 的此项功能：" + permissionPointLabels.getOrDefault(requiredCode, requiredCode));
            }
        }

        return mockMcpClient.invoke(message, requiredTools, trToken);
    }

    private boolean isPermissionPointAllowed(List<AgentStrategyView> strategies, String userId) {
        if (strategies.isEmpty()) {
            return true;
        }
        boolean hasPermit = strategies.stream().anyMatch(strategy -> "PERMIT".equalsIgnoreCase(strategy.effect()));
        boolean permitMatched = false;
        for (AgentStrategyView strategy : strategies) {
            if (!conditionMatches(strategy, userId)) {
                continue;
            }
            if ("DENY".equalsIgnoreCase(strategy.effect())) {
                return false;
            }
            if ("PERMIT".equalsIgnoreCase(strategy.effect())) {
                permitMatched = true;
            }
        }
        if (hasPermit) {
            return permitMatched;
        }
        return true;
    }

    private boolean conditionMatches(AgentStrategyView strategy, String userId) {
        if (strategy.conditions() == null) {
            return true;
        }
        if (!"subject.user_id".equals(strategy.conditions().field())) {
            return false;
        }
        List<String> values = strategy.conditions().values() == null ? List.of() : strategy.conditions().values();
        if ("equals".equalsIgnoreCase(strategy.conditions().operator())) {
            return !values.isEmpty() && userId.equals(values.getFirst());
        }
        if ("in".equalsIgnoreCase(strategy.conditions().operator())) {
            return values.contains(userId);
        }
        return false;
    }

    private DecodedTrContext decodeTrToken(String trToken) {
        try {
            String[] parts = trToken.split("\\.");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid JWT format");
            }
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode payload = objectMapper.readTree(new String(payloadBytes, StandardCharsets.UTF_8));
            JsonNode agencyUser = payload.path("agency_user");
            String userId = agencyUser.path("user_id").asText(null);
            if (userId == null || userId.isBlank()) {
                throw new IllegalArgumentException("TR missing agency_user.user_id");
            }
            Set<String> permissionPointCodes = new LinkedHashSet<>();
            JsonNode authorizedPermissionPoints = agencyUser.path("authorizedPermissionPoints");
            if (authorizedPermissionPoints.isArray()) {
                authorizedPermissionPoints.forEach(node -> {
                    String code = node.path("code").asText();
                    if (!code.isBlank()) {
                        permissionPointCodes.add(code);
                    }
                });
            }
            return new DecodedTrContext(userId, permissionPointCodes);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "当前资源令牌不可解析，无法模拟 MCP 网关校验", exception);
        }
    }

    private ResponseStatusException forbidden(String reason) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, reason);
    }

    private record DecodedTrContext(
            String userId,
            Set<String> authorizedPermissionPointCodes
    ) {
    }
}

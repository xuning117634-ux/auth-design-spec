package com.huawei.it.roma.liveeda.demoagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.it.roma.liveeda.demoagent.client.PolicyCenterClient;
import com.huawei.it.roma.liveeda.demoagent.client.PolicyCenterClient.AgentStrategyView;
import com.huawei.it.roma.liveeda.demoagent.client.PolicyCenterClient.PermissionPointView;
import com.huawei.it.roma.liveeda.demoagent.client.PolicyCenterClient.ResolveByCodesResult;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class MockMcpGatewayClient {

    private final PolicyCenterClient policyCenterClient;
    private final MockMcpClient mockMcpClient;
    private final ObjectMapper objectMapper;

    public MockMcpGatewayClient(
            PolicyCenterClient policyCenterClient,
            MockMcpClient mockMcpClient,
            ObjectMapper objectMapper
    ) {
        this.policyCenterClient = policyCenterClient;
        this.mockMcpClient = mockMcpClient;
        this.objectMapper = objectMapper;
    }

    public Set<String> extractAuthorizedPermissionPointCodes(String trToken) {
        return new LinkedHashSet<>(decodeTrToken(trToken).authorizedPermissionPointCodes());
    }

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

    public String invoke(String agentId, String trToken, Set<String> requiredTools, String message) {
        DecodedTrContext trContext = decodeTrToken(trToken);
        Set<String> trAuthorizedCodes = new LinkedHashSet<>(trContext.authorizedPermissionPointCodes());

        for (String requiredTool : requiredTools.stream().sorted().toList()) {
            PolicyCenterClient.ResolveByToolsResult toolResolution = policyCenterClient.resolveByTools(Set.of(requiredTool));
            Map<String, String> permissionPointLabels = buildPermissionPointLabels(toolResolution.permissionPoints());

            for (String requiredCode : toolResolution.requiredPermissionPointCodes()) {
                if (!trAuthorizedCodes.contains(requiredCode)) {
                    throw forbidden("当前请求缺少所需的用户授权：" + permissionPointLabels.getOrDefault(requiredCode, requiredCode));
                }
            }

            Map<String, List<AgentStrategyView>> strategiesByCode = policyCenterClient
                    .queryAgentStrategies(agentId, toolResolution.requiredPermissionPointCodes())
                    .strategies()
                    .stream()
                    .collect(Collectors.groupingBy(
                            AgentStrategyView::permissionPointCode,
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            for (String requiredCode : toolResolution.requiredPermissionPointCodes()) {
                List<AgentStrategyView> strategies = strategiesByCode.getOrDefault(requiredCode, List.of());
                if (!isPermissionPointAllowed(strategies, trContext.userId())) {
                    throw forbidden("当前用户无权使用该 Agent 的此项功能：" + permissionPointLabels.getOrDefault(requiredCode, requiredCode));
                }
            }
        }

        Set<String> allowedToolsFromTr = resolveCoveredTools(trToken);
        for (String requiredTool : requiredTools) {
            if (!allowedToolsFromTr.contains(requiredTool)) {
                throw forbidden("当前资源令牌不允许调用工具：" + requiredTool);
            }
        }

        return mockMcpClient.invoke(message, requiredTools, trToken);
    }

    private Map<String, String> buildPermissionPointLabels(List<PermissionPointView> permissionPoints) {
        Map<String, String> labels = new LinkedHashMap<>();
        if (permissionPoints == null) {
            return labels;
        }
        for (PermissionPointView permissionPoint : permissionPoints) {
            labels.put(permissionPoint.code(), permissionPoint.displayNameZh());
        }
        return labels;
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
            JsonNode consentedScopes = agencyUser.path("consented_scopes");
            if (consentedScopes.isArray()) {
                consentedScopes.forEach(node -> {
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

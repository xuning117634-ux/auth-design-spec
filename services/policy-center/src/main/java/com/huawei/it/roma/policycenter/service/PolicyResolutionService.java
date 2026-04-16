package com.huawei.it.roma.policycenter.service;

import com.huawei.it.roma.policycenter.config.PolicyCatalogProperties;
import com.huawei.it.roma.policycenter.domain.AgentStrategyItem;
import com.huawei.it.roma.policycenter.domain.PermissionPointItem;
import com.huawei.it.roma.policycenter.domain.StrategyCondition;
import com.huawei.it.roma.policycenter.domain.ToolItem;
import com.huawei.it.roma.policycenter.web.QueryAgentStrategiesResponse;
import com.huawei.it.roma.policycenter.web.ResolveByCodesResponse;
import com.huawei.it.roma.policycenter.web.ResolveByToolsResponse;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@RequiredArgsConstructor
public class PolicyResolutionService {

    private final PolicyCatalogProperties policyCatalogProperties;

    private final Map<String, PermissionPointRecord> permissionPointsByCode = new HashMap<>();
    private final Map<String, ToolItem> toolsById = new HashMap<>();
    private final Map<String, Set<String>> toolToPermissionPoints = new HashMap<>();
    private final Map<String, Set<String>> permissionPointToTools = new HashMap<>();
    private final Map<String, List<AgentStrategyItem>> strategiesByAgentId = new HashMap<>();

    @PostConstruct
    void initialize() {
        policyCatalogProperties.getTools().forEach(definition -> toolsById.put(
                definition.getId(),
                new ToolItem(definition.getId(), definition.getDisplayNameZh())
        ));

        policyCatalogProperties.getPermissionPoints().forEach(definition -> {
            PermissionPointItem permissionPointItem = new PermissionPointItem(
                    definition.getCode(),
                    definition.getDisplayNameZh()
            );
            permissionPointsByCode.put(definition.getCode(), new PermissionPointRecord(
                    permissionPointItem,
                    definition.getDescription(),
                    definition.getStatus(),
                    List.copyOf(definition.getBoundTools())
            ));
            definition.getBoundTools().forEach(toolId -> {
                requireTool(toolId);
                toolToPermissionPoints.computeIfAbsent(toolId, ignored -> new LinkedHashSet<>()).add(definition.getCode());
                permissionPointToTools.computeIfAbsent(definition.getCode(), ignored -> new LinkedHashSet<>()).add(toolId);
            });
        });

        policyCatalogProperties.getAgentStrategies().forEach(definition -> {
            requirePermissionPoint(definition.getPermissionPointCode());
            AgentStrategyItem strategyItem = new AgentStrategyItem(
                    definition.getStrategyId(),
                    definition.getAgentId(),
                    definition.getPermissionPointCode(),
                    new StrategyCondition(
                            definition.getConditions().getField(),
                            definition.getConditions().getOperator(),
                            List.copyOf(definition.getConditions().getValues())
                    ),
                    definition.getEffect(),
                    definition.getStatus()
            );
            strategiesByAgentId.computeIfAbsent(definition.getAgentId(), ignored -> new ArrayList<>()).add(strategyItem);
        });
    }

    public ResolveByToolsResponse resolveByTools(String agentId, List<String> requiredTools) {
        if (CollectionUtils.isEmpty(requiredTools)) {
            throw new PolicyResolutionException("required_tools must not be empty");
        }
        List<String> stableTools = sanitizeAndSort(requiredTools);
        stableTools.forEach(this::requireTool);

        List<String> requiredPermissionPointCodes = stableTools.stream()
                .map(toolId -> toolToPermissionPoints.getOrDefault(toolId, Set.of()))
                .flatMap(Collection::stream)
                .distinct()
                .sorted()
                .toList();

        if (requiredPermissionPointCodes.isEmpty()) {
            throw new PolicyResolutionException("No permission point mapping found for tools: " + stableTools);
        }

        List<PermissionPointItem> permissionPoints = requiredPermissionPointCodes.stream()
                .map(this::requirePermissionPoint)
                .sorted(Comparator.comparing(PermissionPointItem::code))
                .toList();

        return new ResolveByToolsResponse(agentId, requiredPermissionPointCodes, permissionPoints);
    }

    public ResolveByCodesResponse resolveByCodes(List<String> permissionPointCodes) {
        if (CollectionUtils.isEmpty(permissionPointCodes)) {
            throw new PolicyResolutionException("permission_point_codes must not be empty");
        }
        List<String> stableCodes = sanitizeAndSort(permissionPointCodes);
        stableCodes.forEach(this::requirePermissionPointCode);

        List<String> allowedTools = stableCodes.stream()
                .map(code -> permissionPointToTools.getOrDefault(code, Set.of()))
                .flatMap(Collection::stream)
                .distinct()
                .sorted()
                .toList();

        if (allowedTools.isEmpty()) {
            throw new PolicyResolutionException("No tool mapping found for permission_point_codes: " + stableCodes);
        }

        List<PermissionPointItem> permissionPoints = stableCodes.stream()
                .map(this::requirePermissionPoint)
                .sorted(Comparator.comparing(PermissionPointItem::code))
                .toList();

        List<ToolItem> toolItems = allowedTools.stream()
                .map(this::requireTool)
                .sorted(Comparator.comparing(ToolItem::toolId))
                .toList();

        return new ResolveByCodesResponse(stableCodes, allowedTools, permissionPoints, toolItems);
    }

    public QueryAgentStrategiesResponse queryAgentStrategies(String agentId, List<String> permissionPointCodes) {
        if (CollectionUtils.isEmpty(permissionPointCodes)) {
            throw new PolicyResolutionException("permission_point_codes must not be empty");
        }
        List<String> stableCodes = sanitizeAndSort(permissionPointCodes);
        stableCodes.forEach(this::requirePermissionPointCode);

        List<AgentStrategyItem> strategies = strategiesByAgentId.getOrDefault(agentId, List.of()).stream()
                .filter(strategy -> "ACTIVE".equalsIgnoreCase(strategy.status()))
                .filter(strategy -> stableCodes.contains(strategy.permissionPointCode()))
                .sorted(Comparator
                        .comparing(AgentStrategyItem::permissionPointCode)
                        .thenComparing(AgentStrategyItem::strategyId))
                .toList();

        return new QueryAgentStrategiesResponse(agentId, stableCodes, strategies);
    }

    private PermissionPointItem requirePermissionPoint(String code) {
        PermissionPointRecord record = permissionPointsByCode.get(code);
        if (record == null) {
            throw new PolicyResolutionException("Unknown permission_point_code: " + code);
        }
        return record.permissionPoint();
    }

    private void requirePermissionPointCode(String code) {
        requirePermissionPoint(code);
    }

    private ToolItem requireTool(String toolId) {
        ToolItem toolItem = toolsById.get(toolId);
        if (toolItem == null) {
            throw new PolicyResolutionException("Unknown tool_id: " + toolId);
        }
        return toolItem;
    }

    private List<String> sanitizeAndSort(List<String> values) {
        List<String> sanitized = new ArrayList<>();
        values.stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .sorted()
                .distinct()
                .forEach(sanitized::add);
        return sanitized;
    }

    private record PermissionPointRecord(
            PermissionPointItem permissionPoint,
            String description,
            String status,
            List<String> boundTools
    ) {
    }
}

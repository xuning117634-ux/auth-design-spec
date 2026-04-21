package com.huawei.it.roma.policycenter.service;

import com.huawei.it.roma.policycenter.config.PolicyCatalogProperties;
import com.huawei.it.roma.policycenter.domain.AgentStrategyItem;
import com.huawei.it.roma.policycenter.domain.PermissionPointItem;
import com.huawei.it.roma.policycenter.domain.StrategyCondition;
import com.huawei.it.roma.policycenter.domain.ToolItem;
import com.huawei.it.roma.policycenter.web.AgentStrategyBatchUpsertRequest;
import com.huawei.it.roma.policycenter.web.AgentStrategyBatchUpsertResponse;
import com.huawei.it.roma.policycenter.web.PermissionPointBatchUpsertRequest;
import com.huawei.it.roma.policycenter.web.PermissionPointBatchUpsertResponse;
import com.huawei.it.roma.policycenter.web.QueryAgentStrategiesResponse;
import com.huawei.it.roma.policycenter.web.ResolveByCodesResponse;
import com.huawei.it.roma.policycenter.web.ResolveByToolsResponse;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class PolicyResolutionService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_INACTIVE = "INACTIVE";
    private static final Set<String> SUPPORTED_STATUSES = Set.of(STATUS_ACTIVE, STATUS_INACTIVE);
    private static final Set<String> SUPPORTED_EFFECTS = Set.of("PERMIT", "DENY");
    private static final Set<String> SUPPORTED_OPERATORS = Set.of("equals", "in");

    private final PolicyCatalogProperties policyCatalogProperties;

    private final Map<String, ToolItem> toolsById = new LinkedHashMap<>();
    private final Map<String, StoredPermissionPoint> storedPermissionPointsByCode = new LinkedHashMap<>();
    private final Map<String, Map<String, StoredAgentStrategy>> storedStrategiesByAgentId = new LinkedHashMap<>();

    private final Map<String, PermissionPointItem> activePermissionPointsByCode = new LinkedHashMap<>();
    private final Map<String, Set<String>> activeToolToPermissionPoints = new LinkedHashMap<>();
    private final Map<String, Set<String>> activePermissionPointToTools = new LinkedHashMap<>();
    private final Map<String, List<AgentStrategyItem>> activeStrategiesByAgentId = new LinkedHashMap<>();

    public PolicyResolutionService(PolicyCatalogProperties policyCatalogProperties) {
        this.policyCatalogProperties = policyCatalogProperties;
    }

    @PostConstruct
    public synchronized void initialize() {
        toolsById.clear();
        storedPermissionPointsByCode.clear();
        storedStrategiesByAgentId.clear();

        policyCatalogProperties.getTools().forEach(definition -> {
            String toolId = normalizeValue(definition.getId(), "tool_id");
            toolsById.put(toolId, new ToolItem(toolId, normalizeValue(definition.getDisplayNameZh(), "display_name_zh")));
        });

        policyCatalogProperties.getPermissionPoints().forEach(definition -> {
            StoredPermissionPoint storedPermissionPoint = toStoredPermissionPoint(
                    definition.getCode(),
                    definition.getDisplayNameZh(),
                    definition.getDescription(),
                    definition.getBoundTools(),
                    definition.getStatus()
            );
            storedPermissionPointsByCode.put(storedPermissionPoint.code(), storedPermissionPoint);
        });

        policyCatalogProperties.getAgentStrategies().forEach(definition -> {
            StoredAgentStrategy storedAgentStrategy = toStoredAgentStrategy(
                    definition.getAgentId(),
                    definition.getStrategyId(),
                    definition.getPermissionPointCode(),
                    definition.getConditions().getField(),
                    definition.getConditions().getOperator(),
                    definition.getConditions().getValues(),
                    definition.getEffect(),
                    definition.getStatus()
            );
            putStoredStrategy(storedAgentStrategy);
        });

        rebuildActiveIndexes();
    }

    public synchronized PermissionPointBatchUpsertResponse upsertPermissionPoints(PermissionPointBatchUpsertRequest request) {
        if (request == null || CollectionUtils.isEmpty(request.items())) {
            throw new PolicyResolutionException("permission point items must not be empty");
        }
        normalizeValue(request.source(), "source");

        List<StoredPermissionPoint> storedPermissionPoints = request.items().stream()
                .map(item -> toStoredPermissionPoint(
                        item.permissionPointCode(),
                        item.displayNameZh(),
                        item.description(),
                        item.boundTools(),
                        item.status()
                ))
                .sorted(Comparator.comparing(StoredPermissionPoint::code))
                .toList();

        storedPermissionPoints.forEach(item -> storedPermissionPointsByCode.put(item.code(), item));
        rebuildActiveIndexes();

        List<PermissionPointBatchUpsertResponse.ItemResult> results = storedPermissionPoints.stream()
                .map(item -> new PermissionPointBatchUpsertResponse.ItemResult(item.code(), "UPSERTED"))
                .toList();
        return new PermissionPointBatchUpsertResponse(results.size(), results);
    }

    public synchronized AgentStrategyBatchUpsertResponse upsertAgentStrategies(AgentStrategyBatchUpsertRequest request) {
        if (request == null || CollectionUtils.isEmpty(request.items())) {
            throw new PolicyResolutionException("agent strategy items must not be empty");
        }
        String agentId = normalizeValue(request.agentId(), "agent_id");

        List<StoredAgentStrategy> storedStrategies = request.items().stream()
                .map(item -> toStoredAgentStrategy(
                        agentId,
                        item.strategyId(),
                        item.permissionPointCode(),
                        item.conditions().field(),
                        item.conditions().operator(),
                        item.conditions().values(),
                        item.effect(),
                        item.status()
                ))
                .sorted(Comparator.comparing(StoredAgentStrategy::strategyId))
                .toList();

        storedStrategies.forEach(this::putStoredStrategy);
        rebuildActiveIndexes();

        List<AgentStrategyBatchUpsertResponse.ItemResult> results = storedStrategies.stream()
                .map(item -> new AgentStrategyBatchUpsertResponse.ItemResult(item.strategyId(), "UPSERTED"))
                .toList();
        return new AgentStrategyBatchUpsertResponse(agentId, results.size(), results);
    }

    public synchronized ResolveByToolsResponse resolveByTools(List<String> requiredTools) {
        List<String> stableTools = sanitizeAndSort(requiredTools, "required_tools");
        stableTools.forEach(this::requireTool);

        List<String> requiredPermissionPointCodes = stableTools.stream()
                .map(toolId -> activeToolToPermissionPoints.getOrDefault(toolId, Set.of()))
                .flatMap(Collection::stream)
                .distinct()
                .sorted()
                .toList();

        if (requiredPermissionPointCodes.isEmpty()) {
            throw new PolicyResolutionException("No active permission point mapping found for tools: " + stableTools);
        }

        List<PermissionPointItem> permissionPoints = requiredPermissionPointCodes.stream()
                .map(this::requireActivePermissionPoint)
                .sorted(Comparator.comparing(PermissionPointItem::code))
                .toList();

        return new ResolveByToolsResponse(requiredPermissionPointCodes, permissionPoints);
    }

    public synchronized ResolveByCodesResponse resolveByCodes(List<String> permissionPointCodes) {
        List<String> stableCodes = sanitizeAndSort(permissionPointCodes, "permission_point_codes");
        stableCodes.forEach(this::requireActivePermissionPointCode);

        List<ToolItem> tools = stableCodes.stream()
                .map(code -> activePermissionPointToTools.getOrDefault(code, Set.of()))
                .flatMap(Collection::stream)
                .distinct()
                .sorted()
                .map(this::requireTool)
                .sorted(Comparator.comparing(ToolItem::toolId))
                .toList();

        if (tools.isEmpty()) {
            throw new PolicyResolutionException("No active tool mapping found for permission_point_codes: " + stableCodes);
        }

        List<PermissionPointItem> permissionPoints = stableCodes.stream()
                .map(this::requireActivePermissionPoint)
                .sorted(Comparator.comparing(PermissionPointItem::code))
                .toList();

        return new ResolveByCodesResponse(stableCodes, permissionPoints, tools);
    }

    public synchronized QueryAgentStrategiesResponse queryAgentStrategies(String agentId, List<String> permissionPointCodes) {
        String sanitizedAgentId = normalizeValue(agentId, "agent_id");
        List<String> stableCodes = sanitizeAndSort(permissionPointCodes, "permission_point_codes");
        stableCodes.forEach(this::requireActivePermissionPointCode);

        List<AgentStrategyItem> strategies = activeStrategiesByAgentId
                .getOrDefault(sanitizedAgentId, List.of())
                .stream()
                .filter(strategy -> stableCodes.contains(strategy.permissionPointCode()))
                .sorted(Comparator.comparing(AgentStrategyItem::permissionPointCode)
                        .thenComparing(AgentStrategyItem::strategyId))
                .toList();

        return new QueryAgentStrategiesResponse(sanitizedAgentId, stableCodes, strategies);
    }

    private StoredPermissionPoint toStoredPermissionPoint(
            String code,
            String displayNameZh,
            String description,
            List<String> boundTools,
            String status
    ) {
        String normalizedCode = normalizeValue(code, "permission_point_code");
        List<String> sanitizedTools = sanitizeAndSort(boundTools, "bound_tools");
        sanitizedTools.forEach(this::requireTool);
        return new StoredPermissionPoint(
                normalizedCode,
                normalizeValue(displayNameZh, "display_name_zh"),
                normalizeValue(description, "description"),
                sanitizedTools,
                normalizeStatus(status)
        );
    }

    private StoredAgentStrategy toStoredAgentStrategy(
            String agentId,
            String strategyId,
            String permissionPointCode,
            String field,
            String operator,
            List<String> values,
            String effect,
            String status
    ) {
        String normalizedAgentId = normalizeValue(agentId, "agent_id");
        String normalizedPermissionPointCode = normalizeValue(permissionPointCode, "permission_point_code");
        requireStoredPermissionPoint(normalizedPermissionPointCode);
        StrategyCondition condition = new StrategyCondition(
                normalizeConditionField(field),
                normalizeOperator(operator),
                sanitizeAndSort(values, "condition_values")
        );
        return new StoredAgentStrategy(
                normalizeValue(strategyId, "strategy_id"),
                normalizedAgentId,
                normalizedPermissionPointCode,
                condition,
                normalizeEffect(effect),
                normalizeStatus(status)
        );
    }

    private void putStoredStrategy(StoredAgentStrategy storedAgentStrategy) {
        storedStrategiesByAgentId
                .computeIfAbsent(storedAgentStrategy.agentId(), ignored -> new LinkedHashMap<>())
                .put(storedAgentStrategy.strategyId(), storedAgentStrategy);
    }

    private void rebuildActiveIndexes() {
        activePermissionPointsByCode.clear();
        activeToolToPermissionPoints.clear();
        activePermissionPointToTools.clear();
        activeStrategiesByAgentId.clear();

        storedPermissionPointsByCode.values().stream()
                .filter(permissionPoint -> STATUS_ACTIVE.equals(permissionPoint.status()))
                .sorted(Comparator.comparing(StoredPermissionPoint::code))
                .forEach(permissionPoint -> {
                    PermissionPointItem permissionPointItem = new PermissionPointItem(
                            permissionPoint.code(),
                            permissionPoint.displayNameZh()
                    );
                    activePermissionPointsByCode.put(permissionPoint.code(), permissionPointItem);
                    permissionPoint.boundTools().forEach(toolId -> {
                        activeToolToPermissionPoints
                                .computeIfAbsent(toolId, ignored -> new LinkedHashSet<>())
                                .add(permissionPoint.code());
                        activePermissionPointToTools
                                .computeIfAbsent(permissionPoint.code(), ignored -> new LinkedHashSet<>())
                                .add(toolId);
                    });
                });

        storedStrategiesByAgentId.forEach((agentId, strategiesById) -> {
            List<AgentStrategyItem> activeStrategies = strategiesById.values().stream()
                    .filter(strategy -> STATUS_ACTIVE.equals(strategy.status()))
                    .filter(strategy -> activePermissionPointsByCode.containsKey(strategy.permissionPointCode()))
                    .map(strategy -> new AgentStrategyItem(
                            strategy.strategyId(),
                            strategy.agentId(),
                            strategy.permissionPointCode(),
                            strategy.conditions(),
                            strategy.effect(),
                            strategy.status()
                    ))
                    .sorted(Comparator.comparing(AgentStrategyItem::permissionPointCode)
                            .thenComparing(AgentStrategyItem::strategyId))
                    .toList();
            if (!activeStrategies.isEmpty()) {
                activeStrategiesByAgentId.put(agentId, activeStrategies);
            }
        });
    }

    private PermissionPointItem requireStoredPermissionPoint(String code) {
        StoredPermissionPoint permissionPoint = storedPermissionPointsByCode.get(code);
        if (permissionPoint == null) {
            throw new PolicyResolutionException("Unknown permission_point_code: " + code);
        }
        return new PermissionPointItem(permissionPoint.code(), permissionPoint.displayNameZh());
    }

    private PermissionPointItem requireActivePermissionPoint(String code) {
        PermissionPointItem permissionPointItem = activePermissionPointsByCode.get(code);
        if (permissionPointItem == null) {
            throw new PolicyResolutionException("Unknown or inactive permission_point_code: " + code);
        }
        return permissionPointItem;
    }

    private void requireActivePermissionPointCode(String code) {
        requireActivePermissionPoint(code);
    }

    private ToolItem requireTool(String toolId) {
        ToolItem toolItem = toolsById.get(toolId);
        if (toolItem == null) {
            throw new PolicyResolutionException("Unknown tool_id: " + toolId);
        }
        return toolItem;
    }

    private String normalizeConditionField(String field) {
        String normalizedField = normalizeValue(field, "condition.field");
        if (!"subject.user_id".equals(normalizedField)) {
            throw new PolicyResolutionException("Unsupported condition field: " + normalizedField);
        }
        return normalizedField;
    }

    private String normalizeOperator(String operator) {
        String normalizedOperator = normalizeValue(operator, "condition.operator").toLowerCase();
        if (!SUPPORTED_OPERATORS.contains(normalizedOperator)) {
            throw new PolicyResolutionException("Unsupported condition operator: " + normalizedOperator);
        }
        return normalizedOperator;
    }

    private String normalizeEffect(String effect) {
        String normalizedEffect = normalizeValue(effect, "effect").toUpperCase();
        if (!SUPPORTED_EFFECTS.contains(normalizedEffect)) {
            throw new PolicyResolutionException("Unsupported effect: " + normalizedEffect);
        }
        return normalizedEffect;
    }

    private String normalizeStatus(String status) {
        String normalizedStatus = normalizeValue(status, "status").toUpperCase();
        if (!SUPPORTED_STATUSES.contains(normalizedStatus)) {
            throw new PolicyResolutionException("Unsupported status: " + normalizedStatus);
        }
        return normalizedStatus;
    }

    private String normalizeValue(String value, String fieldName) {
        String normalizedValue = value == null ? "" : value.trim();
        if (normalizedValue.isEmpty()) {
            throw new PolicyResolutionException(fieldName + " must not be blank");
        }
        return normalizedValue;
    }

    private List<String> sanitizeAndSort(List<String> values, String fieldName) {
        if (CollectionUtils.isEmpty(values)) {
            throw new PolicyResolutionException(fieldName + " must not be empty");
        }
        List<String> sanitized = new ArrayList<>();
        values.stream()
                .map(value -> value == null ? "" : value.trim())
                .filter(value -> !value.isEmpty())
                .sorted()
                .distinct()
                .forEach(sanitized::add);
        if (sanitized.isEmpty()) {
            throw new PolicyResolutionException(fieldName + " must not be empty");
        }
        return sanitized;
    }

    private record StoredPermissionPoint(
            String code,
            String displayNameZh,
            String description,
            List<String> boundTools,
            String status
    ) {
    }

    private record StoredAgentStrategy(
            String strategyId,
            String agentId,
            String permissionPointCode,
            StrategyCondition conditions,
            String effect,
            String status
    ) {
    }
}

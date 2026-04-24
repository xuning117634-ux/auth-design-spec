package com.huawei.it.roma.policycenter.service;

import com.huawei.it.roma.policycenter.domain.AgentStrategyItem;
import com.huawei.it.roma.policycenter.domain.PermissionPointItem;
import com.huawei.it.roma.policycenter.domain.StrategyCondition;
import com.huawei.it.roma.policycenter.domain.ToolItem;
import com.huawei.it.roma.policycenter.persistence.mapper.AgentStrategyMapper;
import com.huawei.it.roma.policycenter.persistence.mapper.PermissionPointMapper;
import com.huawei.it.roma.policycenter.persistence.mapper.PermissionPointToolRelMapper;
import com.huawei.it.roma.policycenter.persistence.mapper.StrategyConditionValueMapper;
import com.huawei.it.roma.policycenter.persistence.mapper.ToolMapper;
import com.huawei.it.roma.policycenter.persistence.model.AgentStrategyRow;
import com.huawei.it.roma.policycenter.persistence.model.PermissionPointRow;
import com.huawei.it.roma.policycenter.persistence.model.PermissionPointToolRelRow;
import com.huawei.it.roma.policycenter.persistence.model.StrategyConditionValueRow;
import com.huawei.it.roma.policycenter.persistence.model.ToolRow;
import com.huawei.it.roma.policycenter.web.AgentStrategyBatchUpsertRequest;
import com.huawei.it.roma.policycenter.web.AgentStrategyBatchUpsertResponse;
import com.huawei.it.roma.policycenter.web.PermissionPointBatchUpsertRequest;
import com.huawei.it.roma.policycenter.web.PermissionPointBatchUpsertResponse;
import com.huawei.it.roma.policycenter.web.QueryAgentStrategiesResponse;
import com.huawei.it.roma.policycenter.web.ResolveByCodesResponse;
import com.huawei.it.roma.policycenter.web.ResolveByToolsResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
public class PolicyResolutionService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_INACTIVE = "INACTIVE";
    private static final Set<String> SUPPORTED_STATUSES = Set.of(STATUS_ACTIVE, STATUS_INACTIVE);
    private static final Set<String> SUPPORTED_EFFECTS = Set.of("PERMIT", "DENY");
    private static final Set<String> SUPPORTED_OPERATORS = Set.of("equals", "in");
    private static final String SUPPORTED_CONDITION_FIELD = "subject.user_id";

    private final PermissionPointMapper permissionPointMapper;
    private final ToolMapper toolMapper;
    private final PermissionPointToolRelMapper permissionPointToolRelMapper;
    private final AgentStrategyMapper agentStrategyMapper;
    private final StrategyConditionValueMapper strategyConditionValueMapper;

    public PolicyResolutionService(
            PermissionPointMapper permissionPointMapper,
            ToolMapper toolMapper,
            PermissionPointToolRelMapper permissionPointToolRelMapper,
            AgentStrategyMapper agentStrategyMapper,
            StrategyConditionValueMapper strategyConditionValueMapper
    ) {
        this.permissionPointMapper = permissionPointMapper;
        this.toolMapper = toolMapper;
        this.permissionPointToolRelMapper = permissionPointToolRelMapper;
        this.agentStrategyMapper = agentStrategyMapper;
        this.strategyConditionValueMapper = strategyConditionValueMapper;
    }

    @Transactional
    public PermissionPointBatchUpsertResponse upsertPermissionPoints(PermissionPointBatchUpsertRequest request) {
        if (request == null || CollectionUtils.isEmpty(request.items())) {
            throw new PolicyResolutionException("permission point items must not be empty");
        }
        String source = normalizeValue(request.source(), "source");
        List<NormalizedPermissionPoint> permissionPoints = request.items().stream()
                .map(item -> normalizePermissionPoint(item, source))
                .sorted(Comparator.comparing(NormalizedPermissionPoint::permissionPointCode))
                .toList();

        permissionPoints.forEach(this::upsertPermissionPoint);

        List<PermissionPointBatchUpsertResponse.ItemResult> results = permissionPoints.stream()
                .map(item -> new PermissionPointBatchUpsertResponse.ItemResult(item.permissionPointCode(), "UPSERTED"))
                .toList();
        return new PermissionPointBatchUpsertResponse(results.size(), results);
    }

    @Transactional
    public AgentStrategyBatchUpsertResponse upsertAgentStrategies(AgentStrategyBatchUpsertRequest request) {
        if (request == null || CollectionUtils.isEmpty(request.items())) {
            throw new PolicyResolutionException("agent strategy items must not be empty");
        }
        String agentId = normalizeValue(request.agentId(), "agent_id");
        List<NormalizedAgentStrategy> strategies = request.items().stream()
                .map(item -> normalizeAgentStrategy(agentId, item))
                .sorted(Comparator.comparing(NormalizedAgentStrategy::strategyId))
                .toList();

        ensurePermissionPointsExist(strategies.stream()
                .map(NormalizedAgentStrategy::permissionPointCode)
                .distinct()
                .sorted()
                .toList());

        strategies.forEach(this::upsertAgentStrategy);

        List<AgentStrategyBatchUpsertResponse.ItemResult> results = strategies.stream()
                .map(item -> new AgentStrategyBatchUpsertResponse.ItemResult(item.strategyId(), "UPSERTED"))
                .toList();
        return new AgentStrategyBatchUpsertResponse(agentId, results.size(), results);
    }

    public ResolveByToolsResponse resolveByTools(List<String> requiredTools) {
        List<String> stableTools = sanitizeAndSort(requiredTools, "required_tools");

        List<PermissionPointToolRelRow> relationRows = permissionPointToolRelMapper.findActiveByToolIds(stableTools);
        if (relationRows.isEmpty()) {
            return new ResolveByToolsResponse(List.of(), List.of());
        }

        List<String> permissionPointCodes = relationRows.stream()
                .map(PermissionPointToolRelRow::getPermissionPointCode)
                .distinct()
                .sorted()
                .toList();
        List<PermissionPointRow> permissionPointRows = permissionPointMapper.findActiveByCodes(permissionPointCodes);
        Map<String, PermissionPointRow> permissionPointsByCode = permissionPointRows.stream()
                .collect(Collectors.toMap(PermissionPointRow::getPermissionPointCode, row -> row));

        List<PermissionPointItem> permissionPoints = permissionPointCodes.stream()
                .map(permissionPointsByCode::get)
                .filter(row -> row != null)
                .map(this::toPermissionPointItem)
                .toList();
        List<String> matchedPermissionPointCodes = permissionPoints.stream()
                .map(PermissionPointItem::code)
                .toList();
        return new ResolveByToolsResponse(matchedPermissionPointCodes, permissionPoints);
    }

    public ResolveByCodesResponse resolveByCodes(List<String> permissionPointCodes) {
        List<String> stableCodes = sanitizeAndSort(permissionPointCodes, "permission_point_codes");
        List<PermissionPointRow> permissionPointRows = permissionPointMapper.findActiveByCodes(stableCodes);
        if (permissionPointRows.isEmpty()) {
            return new ResolveByCodesResponse(List.of(), List.of(), List.of());
        }

        List<String> activeCodes = permissionPointRows.stream()
                .map(PermissionPointRow::getPermissionPointCode)
                .distinct()
                .sorted()
                .toList();
        List<ToolRow> toolRows = toolMapper.findActiveToolsByPermissionPointCodes(activeCodes);
        List<ToolItem> tools = toolRows.stream()
                .sorted(Comparator.comparing(ToolRow::getToolId))
                .map(this::toToolItem)
                .toList();

        List<PermissionPointItem> permissionPoints = permissionPointRows.stream()
                .sorted(Comparator.comparing(PermissionPointRow::getPermissionPointCode))
                .map(this::toPermissionPointItem)
                .toList();
        return new ResolveByCodesResponse(activeCodes, permissionPoints, tools);
    }

    public QueryAgentStrategiesResponse queryAgentStrategies(String agentId, List<String> permissionPointCodes) {
        String normalizedAgentId = normalizeValue(agentId, "agent_id");
        List<String> stableCodes = sanitizeAndSort(permissionPointCodes, "permission_point_codes");
        List<String> activeCodes = permissionPointMapper.findActiveByCodes(stableCodes).stream()
                .map(PermissionPointRow::getPermissionPointCode)
                .distinct()
                .sorted()
                .toList();
        if (activeCodes.isEmpty()) {
            return new QueryAgentStrategiesResponse(normalizedAgentId, List.of(), List.of());
        }

        List<AgentStrategyRow> strategyRows = agentStrategyMapper.findActiveByAgentIdAndPermissionPointCodes(
                normalizedAgentId,
                activeCodes
        );
        if (strategyRows.isEmpty()) {
            return new QueryAgentStrategiesResponse(normalizedAgentId, activeCodes, List.of());
        }

        List<String> strategyIds = strategyRows.stream()
                .map(AgentStrategyRow::getStrategyId)
                .distinct()
                .sorted()
                .toList();
        Map<String, List<String>> conditionValuesByStrategyId = strategyConditionValueMapper.findByStrategyIds(strategyIds).stream()
                .collect(Collectors.groupingBy(
                        StrategyConditionValueRow::getStrategyId,
                        LinkedHashMap::new,
                        Collectors.mapping(StrategyConditionValueRow::getConditionValue, Collectors.toList())
                ));

        List<AgentStrategyItem> strategies = strategyRows.stream()
                .sorted(Comparator.comparing(AgentStrategyRow::getPermissionPointCode)
                        .thenComparing(AgentStrategyRow::getStrategyId))
                .map(row -> new AgentStrategyItem(
                        row.getStrategyId(),
                        row.getAgentId(),
                        row.getPermissionPointCode(),
                        new StrategyCondition(
                                row.getConditionField(),
                                row.getConditionOperator(),
                                List.copyOf(conditionValuesByStrategyId.getOrDefault(row.getStrategyId(), List.of()))
                        ),
                        row.getEffect(),
                        row.getStatus()
                ))
                .toList();
        return new QueryAgentStrategiesResponse(normalizedAgentId, activeCodes, strategies);
    }

    private void upsertPermissionPoint(NormalizedPermissionPoint permissionPoint) {
        PermissionPointRow permissionPointRow = new PermissionPointRow(
                permissionPoint.permissionPointCode(),
                permissionPoint.displayNameZh(),
                permissionPoint.description(),
                permissionPoint.status(),
                permissionPoint.source()
        );
        if (permissionPointMapper.update(permissionPointRow) == 0) {
            permissionPointMapper.insert(permissionPointRow);
        }

        permissionPoint.boundTools().forEach(this::upsertTool);
        permissionPointToolRelMapper.deleteByPermissionPointCode(permissionPoint.permissionPointCode());
        List<PermissionPointToolRelRow> relations = permissionPoint.boundTools().stream()
                .map(tool -> new PermissionPointToolRelRow(permissionPoint.permissionPointCode(), tool.toolId()))
                .toList();
        permissionPointToolRelMapper.insertBatch(relations);
    }

    private void upsertTool(NormalizedBoundTool tool) {
        ToolRow toolRow = new ToolRow(tool.toolId(), tool.displayNameZh());
        if (toolMapper.update(toolRow) == 0) {
            toolMapper.insert(toolRow);
        }
    }

    private void upsertAgentStrategy(NormalizedAgentStrategy strategy) {
        AgentStrategyRow strategyRow = new AgentStrategyRow(
                strategy.strategyId(),
                strategy.agentId(),
                strategy.permissionPointCode(),
                strategy.conditionField(),
                strategy.conditionOperator(),
                strategy.effect(),
                strategy.status()
        );
        if (agentStrategyMapper.update(strategyRow) == 0) {
            agentStrategyMapper.insert(strategyRow);
        }

        strategyConditionValueMapper.deleteByStrategyId(strategy.strategyId());
        List<StrategyConditionValueRow> values = new ArrayList<>();
        for (int index = 0; index < strategy.conditionValues().size(); index++) {
            values.add(new StrategyConditionValueRow(
                    strategy.strategyId(),
                    index,
                    strategy.conditionValues().get(index)
            ));
        }
        strategyConditionValueMapper.insertBatch(values);
    }

    private NormalizedPermissionPoint normalizePermissionPoint(
            PermissionPointBatchUpsertRequest.Item item,
            String source
    ) {
        return new NormalizedPermissionPoint(
                normalizeValue(item.permissionPointCode(), "permission_point_code"),
                normalizeValue(item.displayNameZh(), "display_name_zh"),
                normalizeValue(item.description(), "description"),
                normalizeBoundTools(item.boundTools()),
                normalizeStatus(item.status()),
                source
        );
    }

    private List<NormalizedBoundTool> normalizeBoundTools(List<PermissionPointBatchUpsertRequest.BoundTool> boundTools) {
        if (CollectionUtils.isEmpty(boundTools)) {
            throw new PolicyResolutionException("bound_tools must not be empty");
        }
        List<NormalizedBoundTool> requestedTools = boundTools.stream()
                .map(tool -> new NormalizedBoundTool(
                        normalizeValue(tool.toolId(), "bound_tools.tool_id"),
                        normalizeOptionalValue(tool.displayNameZh())
                ))
                .toList();
        Map<String, String> existingDisplayNamesByToolId = toolMapper.findByToolIds(requestedTools.stream()
                        .map(NormalizedBoundTool::toolId)
                        .distinct()
                        .sorted()
                        .toList()).stream()
                .collect(Collectors.toMap(ToolRow::getToolId, ToolRow::getDisplayNameZh));
        Map<String, NormalizedBoundTool> toolsById = new TreeMap<>();
        requestedTools.forEach(tool -> {
            String displayNameZh = tool.displayNameZh();
            if (displayNameZh == null || displayNameZh.isBlank()) {
                displayNameZh = existingDisplayNamesByToolId.getOrDefault(tool.toolId(), tool.toolId());
            }
            toolsById.put(tool.toolId(), new NormalizedBoundTool(tool.toolId(), displayNameZh));
        });
        if (toolsById.isEmpty()) {
            throw new PolicyResolutionException("bound_tools must not be empty");
        }
        return List.copyOf(toolsById.values());
    }

    private NormalizedAgentStrategy normalizeAgentStrategy(
            String agentId,
            AgentStrategyBatchUpsertRequest.Item item
    ) {
        return new NormalizedAgentStrategy(
                normalizeValue(item.strategyId(), "strategy_id"),
                agentId,
                normalizeValue(item.permissionPointCode(), "permission_point_code"),
                normalizeConditionField(item.conditions().field()),
                normalizeOperator(item.conditions().operator()),
                sanitizeAndSort(item.conditions().values(), "condition_values"),
                normalizeEffect(item.effect()),
                normalizeStatus(item.status())
        );
    }

    private void ensurePermissionPointsExist(List<String> permissionPointCodes) {
        List<PermissionPointRow> rows = permissionPointMapper.findByCodes(permissionPointCodes);
        Set<String> existingCodes = rows.stream()
                .map(PermissionPointRow::getPermissionPointCode)
                .collect(Collectors.toSet());
        permissionPointCodes.forEach(code -> {
            if (!existingCodes.contains(code)) {
                throw new PolicyResolutionException("Unknown permission_point_code: " + code);
            }
        });
    }

    private PermissionPointItem toPermissionPointItem(PermissionPointRow row) {
        return new PermissionPointItem(row.getPermissionPointCode(), row.getDisplayNameZh());
    }

    private ToolItem toToolItem(ToolRow row) {
        return new ToolItem(row.getToolId(), row.getDisplayNameZh());
    }

    private String normalizeConditionField(String field) {
        String normalizedField = normalizeValue(field, "condition.field");
        if (!SUPPORTED_CONDITION_FIELD.equals(normalizedField)) {
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

    private String normalizeOptionalValue(String value) {
        if (value == null) {
            return null;
        }
        String normalizedValue = value.trim();
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }

    private List<String> sanitizeAndSort(List<String> values, String fieldName) {
        if (CollectionUtils.isEmpty(values)) {
            throw new PolicyResolutionException(fieldName + " must not be empty");
        }
        List<String> sanitized = values.stream()
                .map(value -> value == null ? "" : value.trim())
                .filter(value -> !value.isEmpty())
                .distinct()
                .sorted()
                .toList();
        if (sanitized.isEmpty()) {
            throw new PolicyResolutionException(fieldName + " must not be empty");
        }
        return sanitized;
    }

    private record NormalizedPermissionPoint(
            String permissionPointCode,
            String displayNameZh,
            String description,
            List<NormalizedBoundTool> boundTools,
            String status,
            String source
    ) {
    }

    private record NormalizedBoundTool(
            String toolId,
            String displayNameZh
    ) {
    }

    private record NormalizedAgentStrategy(
            String strategyId,
            String agentId,
            String permissionPointCode,
            String conditionField,
            String conditionOperator,
            List<String> conditionValues,
            String effect,
            String status
    ) {
    }
}

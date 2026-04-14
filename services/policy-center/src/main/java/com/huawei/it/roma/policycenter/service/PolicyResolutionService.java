package com.huawei.it.roma.policycenter.service;

import com.huawei.it.roma.policycenter.config.PolicyCatalogProperties;
import com.huawei.it.roma.policycenter.domain.PolicyItem;
import com.huawei.it.roma.policycenter.domain.ToolItem;
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

    private final Map<String, PolicyItem> policiesByCode = new HashMap<>();
    private final Map<String, ToolItem> toolsById = new HashMap<>();
    private final Map<String, Set<String>> toolToCodes = new HashMap<>();
    private final Map<String, Set<String>> codeToTools = new HashMap<>();

    @PostConstruct
    void initialize() {
        policyCatalogProperties.getPolicies()
                .forEach(definition -> policiesByCode.put(
                        definition.getCode(),
                        new PolicyItem(definition.getCode(), definition.getDisplayName())));
        policyCatalogProperties.getTools()
                .forEach(definition -> toolsById.put(
                        definition.getId(),
                        new ToolItem(definition.getId(), definition.getDisplayName())));

        policyCatalogProperties.getMappings().forEach(mapping -> {
            requirePolicy(mapping.getPolicyCode());
            requireTool(mapping.getToolId());
            toolToCodes.computeIfAbsent(mapping.getToolId(), ignored -> new LinkedHashSet<>()).add(mapping.getPolicyCode());
            codeToTools.computeIfAbsent(mapping.getPolicyCode(), ignored -> new LinkedHashSet<>()).add(mapping.getToolId());
        });
    }

    public ResolveByToolsResponse resolveByTools(String agentId, List<String> requiredTools) {
        if (CollectionUtils.isEmpty(requiredTools)) {
            throw new PolicyResolutionException("required_tools must not be empty");
        }
        List<String> stableTools = sanitizeAndSort(requiredTools);
        stableTools.forEach(this::requireTool);

        List<String> requiredPolicyCodes = stableTools.stream()
                .map(toolId -> toolToCodes.getOrDefault(toolId, Set.of()))
                .flatMap(Collection::stream)
                .distinct()
                .sorted()
                .toList();

        if (requiredPolicyCodes.isEmpty()) {
            throw new PolicyResolutionException("No policy_code mapping found for tools: " + stableTools);
        }

        List<PolicyItem> policyItems = requiredPolicyCodes.stream()
                .map(this::requirePolicy)
                .sorted(Comparator.comparing(PolicyItem::policyCode))
                .toList();

        return new ResolveByToolsResponse(agentId, requiredPolicyCodes, policyItems);
    }

    public ResolveByCodesResponse resolveByCodes(List<String> policyCodes) {
        if (CollectionUtils.isEmpty(policyCodes)) {
            throw new PolicyResolutionException("policy_codes must not be empty");
        }
        List<String> stableCodes = sanitizeAndSort(policyCodes);
        stableCodes.forEach(this::requirePolicyCode);

        List<String> allowedTools = stableCodes.stream()
                .map(code -> codeToTools.getOrDefault(code, Set.of()))
                .flatMap(Collection::stream)
                .distinct()
                .sorted()
                .toList();

        if (allowedTools.isEmpty()) {
            throw new PolicyResolutionException("No tool mapping found for policy_codes: " + stableCodes);
        }

        List<ToolItem> toolItems = allowedTools.stream()
                .map(this::requireTool)
                .sorted(Comparator.comparing(ToolItem::toolId))
                .toList();

        return new ResolveByCodesResponse(stableCodes, allowedTools, toolItems);
    }

    private PolicyItem requirePolicy(String policyCode) {
        PolicyItem policyItem = policiesByCode.get(policyCode);
        if (policyItem == null) {
            throw new PolicyResolutionException("Unknown policy_code: " + policyCode);
        }
        return policyItem;
    }

    private void requirePolicyCode(String policyCode) {
        requirePolicy(policyCode);
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
}

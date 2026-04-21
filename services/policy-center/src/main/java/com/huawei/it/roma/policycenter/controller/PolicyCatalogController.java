package com.huawei.it.roma.policycenter.controller;

import com.huawei.it.roma.policycenter.service.PolicyResolutionService;
import com.huawei.it.roma.policycenter.web.AgentStrategyBatchUpsertRequest;
import com.huawei.it.roma.policycenter.web.AgentStrategyBatchUpsertResponse;
import com.huawei.it.roma.policycenter.web.PermissionPointBatchUpsertRequest;
import com.huawei.it.roma.policycenter.web.PermissionPointBatchUpsertResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1")
public class PolicyCatalogController {

    private final PolicyResolutionService policyResolutionService;

    public PolicyCatalogController(PolicyResolutionService policyResolutionService) {
        this.policyResolutionService = policyResolutionService;
    }

    @PutMapping("/permission-points/batch-upsert")
    public PermissionPointBatchUpsertResponse upsertPermissionPoints(
            @Valid @RequestBody PermissionPointBatchUpsertRequest request
    ) {
        return policyResolutionService.upsertPermissionPoints(request);
    }

    @PutMapping("/agent-strategies/batch-upsert")
    public AgentStrategyBatchUpsertResponse upsertAgentStrategies(
            @Valid @RequestBody AgentStrategyBatchUpsertRequest request
    ) {
        return policyResolutionService.upsertAgentStrategies(request);
    }
}

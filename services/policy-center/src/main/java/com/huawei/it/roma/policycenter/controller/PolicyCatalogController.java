package com.huawei.it.roma.policycenter.controller;

import com.huawei.it.roma.policycenter.service.PolicyResolutionService;
import com.huawei.it.roma.policycenter.web.AgentPermissionPointBatchUpsertRequest;
import com.huawei.it.roma.policycenter.web.AgentPermissionPointBatchUpsertResponse;
import com.huawei.it.roma.policycenter.web.AgentStrategyBatchHardDeleteRequest;
import com.huawei.it.roma.policycenter.web.AgentStrategyBatchUpsertRequest;
import com.huawei.it.roma.policycenter.web.AgentStrategyBatchUpsertResponse;
import com.huawei.it.roma.policycenter.web.BatchHardDeleteResponse;
import com.huawei.it.roma.policycenter.web.PermissionPointBatchHardDeleteRequest;
import com.huawei.it.roma.policycenter.web.PermissionPointBatchUpsertRequest;
import com.huawei.it.roma.policycenter.web.PermissionPointBatchUpsertResponse;
import com.huawei.it.roma.policycenter.web.QueryPermissionPointsRequest;
import com.huawei.it.roma.policycenter.web.QueryPermissionPointsResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
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

    @PostMapping("/permission-points/query")
    public QueryPermissionPointsResponse queryPermissionPoints(
            @Valid @RequestBody QueryPermissionPointsRequest request
    ) {
        return policyResolutionService.queryPermissionPoints(request);
    }

    @PostMapping("/permission-points/batch-hard-delete")
    public BatchHardDeleteResponse hardDeletePermissionPoints(
            @Valid @RequestBody PermissionPointBatchHardDeleteRequest request
    ) {
        return policyResolutionService.hardDeletePermissionPoints(request);
    }

    @PutMapping("/agent-permission-points/batch-upsert")
    public AgentPermissionPointBatchUpsertResponse upsertAgentPermissionPoints(
            @Valid @RequestBody AgentPermissionPointBatchUpsertRequest request
    ) {
        return policyResolutionService.upsertAgentPermissionPoints(request);
    }

    @PutMapping("/agent-strategies/batch-upsert")
    public AgentStrategyBatchUpsertResponse upsertAgentStrategies(
            @Valid @RequestBody AgentStrategyBatchUpsertRequest request
    ) {
        return policyResolutionService.upsertAgentStrategies(request);
    }

    @PostMapping("/agent-strategies/batch-hard-delete")
    public BatchHardDeleteResponse hardDeleteAgentStrategies(
            @Valid @RequestBody AgentStrategyBatchHardDeleteRequest request
    ) {
        return policyResolutionService.hardDeleteAgentStrategies(request);
    }
}

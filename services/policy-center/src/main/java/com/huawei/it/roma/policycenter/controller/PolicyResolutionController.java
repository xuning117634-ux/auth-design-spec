package com.huawei.it.roma.policycenter.controller;

import com.huawei.it.roma.policycenter.service.PolicyResolutionService;
import com.huawei.it.roma.policycenter.web.QueryAgentStrategiesRequest;
import com.huawei.it.roma.policycenter.web.QueryAgentStrategiesResponse;
import com.huawei.it.roma.policycenter.web.ResolveByCodesRequest;
import com.huawei.it.roma.policycenter.web.ResolveByCodesResponse;
import com.huawei.it.roma.policycenter.web.ResolveByToolsRequest;
import com.huawei.it.roma.policycenter.web.ResolveByToolsResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1")
@RequiredArgsConstructor
public class PolicyResolutionController {

    private final PolicyResolutionService policyResolutionService;

    @PostMapping("/permission-points/resolve-by-tools")
    public ResolveByToolsResponse resolveByTools(@Valid @RequestBody ResolveByToolsRequest request) {
        return policyResolutionService.resolveByTools(request.agentId(), request.requiredTools());
    }

    @PostMapping("/permission-points/resolve-by-codes")
    public ResolveByCodesResponse resolveByCodes(@Valid @RequestBody ResolveByCodesRequest request) {
        return policyResolutionService.resolveByCodes(request.permissionPointCodes());
    }

    @PostMapping("/agent-strategies/query")
    public QueryAgentStrategiesResponse queryAgentStrategies(@Valid @RequestBody QueryAgentStrategiesRequest request) {
        return policyResolutionService.queryAgentStrategies(request.agentId(), request.permissionPointCodes());
    }
}

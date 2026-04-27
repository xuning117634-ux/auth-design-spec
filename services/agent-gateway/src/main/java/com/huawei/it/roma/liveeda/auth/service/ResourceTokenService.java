package com.huawei.it.roma.liveeda.auth.service;

import com.huawei.it.roma.liveeda.auth.client.AgentManagementClient;
import com.huawei.it.roma.liveeda.auth.client.PolicyCenterClient;
import com.huawei.it.roma.liveeda.auth.client.PolicyResolutionResult;
import com.huawei.it.roma.liveeda.auth.config.AgentGatewayProperties;
import com.huawei.it.roma.liveeda.auth.domain.AgentRegistryEntry;
import com.huawei.it.roma.liveeda.auth.domain.PendingAuthTransaction;
import com.huawei.it.roma.liveeda.auth.store.PendingAuthTransactionStore;
import com.huawei.it.roma.liveeda.auth.util.IdGenerator;
import com.huawei.it.roma.liveeda.auth.web.GatewayException;
import com.huawei.it.roma.liveeda.auth.web.ResourceTokenRequest;
import com.huawei.it.roma.liveeda.auth.web.ResourceTokenResponse;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class ResourceTokenService {

    private final AgentManagementClient agentManagementClient;
    private final PolicyCenterClient policyCenterClient;
    private final PendingAuthTransactionStore pendingAuthTransactionStore;
    private final ReturnUrlValidator returnUrlValidator;
    private final AgentGatewayProperties properties;
    private final IdGenerator idGenerator;

    public ResourceTokenResponse issueResourceToken(ResourceTokenRequest request) {
        AgentRegistryEntry agentRegistryEntry = agentManagementClient.getGatewayProfile(request.agentId());
        URI validatedReturnUrl = returnUrlValidator.validate(agentRegistryEntry, request.returnUrl());

        Set<String> requiredTools = sanitizeTools(request.requiredTools());
        PolicyResolutionResult policyResolutionResult = policyCenterClient.resolveByTools(requiredTools);
        ensureSubscribedPermissionPoints(agentRegistryEntry, policyResolutionResult.requiredPermissionPointCodes());

        String requestId = idGenerator.next("req");
        pendingAuthTransactionStore.save(new PendingAuthTransaction(
                requestId,
                request.agentId(),
                requiredTools,
                policyResolutionResult.requiredPermissionPointCodes(),
                policyResolutionResult.permissionPoints(),
                validatedReturnUrl,
                request.state(),
                request.subjectHint(),
                null
        ));

        String redirectUrl = UriComponentsBuilder.fromHttpUrl(properties.getSelfBaseUrl())
                .path("/gw/auth/authorize")
                .queryParam("request_id", requestId)
                .build(true)
                .toUriString();

        return ResourceTokenResponse.redirect(redirectUrl, requestId);
    }

    private void ensureSubscribedPermissionPoints(
            AgentRegistryEntry agentRegistryEntry,
            Set<String> requiredPermissionPointCodes
    ) {
        if (!agentRegistryEntry.hasSubscribedAll(requiredPermissionPointCodes)) {
            throw new GatewayException(
                    HttpStatus.FORBIDDEN,
                    "Agent has not subscribed all required permission points"
            );
        }
    }

    private Set<String> sanitizeTools(Iterable<String> values) {
        Set<String> requiredTools = new LinkedHashSet<>();
        values.forEach(value -> {
            String sanitized = value == null ? "" : value.trim();
            if (!sanitized.isEmpty()) {
                requiredTools.add(sanitized);
            }
        });
        if (requiredTools.isEmpty()) {
            throw new GatewayException(HttpStatus.BAD_REQUEST, "required_tools must not be empty");
        }
        return requiredTools;
    }
}

package com.huawei.it.roma.liveeda.auth.service;

import com.huawei.it.roma.liveeda.auth.client.PolicyCenterClient;
import com.huawei.it.roma.liveeda.auth.client.PolicyResolutionResult;
import com.huawei.it.roma.liveeda.auth.config.AgentGatewayProperties;
import com.huawei.it.roma.liveeda.auth.domain.AgentRegistryEntry;
import com.huawei.it.roma.liveeda.auth.domain.GatewayAuthContext;
import com.huawei.it.roma.liveeda.auth.domain.GatewaySession;
import com.huawei.it.roma.liveeda.auth.domain.PendingAuthTransaction;
import com.huawei.it.roma.liveeda.auth.store.AgentRegistryStore;
import com.huawei.it.roma.liveeda.auth.store.GatewayAuthContextStore;
import com.huawei.it.roma.liveeda.auth.store.GatewaySessionStore;
import com.huawei.it.roma.liveeda.auth.store.PendingAuthTransactionStore;
import com.huawei.it.roma.liveeda.auth.util.IdGenerator;
import com.huawei.it.roma.liveeda.auth.web.GatewayException;
import com.huawei.it.roma.liveeda.auth.web.ResourceTokenRequest;
import com.huawei.it.roma.liveeda.auth.web.ResourceTokenResponse;
import java.net.URI;
import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class ResourceTokenService {

    private final GatewaySessionStore gatewaySessionStore;
    private final AgentRegistryStore agentRegistryStore;
    private final PolicyCenterClient policyCenterClient;
    private final GatewayAuthContextStore gatewayAuthContextStore;
    private final PendingAuthTransactionStore pendingAuthTransactionStore;
    private final ReturnUrlValidator returnUrlValidator;
    private final AgentGatewayProperties properties;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public ResourceTokenResponse issueResourceToken(String gwSessionToken, ResourceTokenRequest request) {
        GatewaySession gatewaySession = gatewaySessionStore.findByToken(gwSessionToken)
                .orElseThrow(() -> new GatewayException(HttpStatus.UNAUTHORIZED, "Invalid gw_session_token"));

        AgentRegistryEntry agentRegistryEntry = agentRegistryStore.require(request.agentId());
        if (!agentRegistryEntry.isActive()) {
            throw new GatewayException(HttpStatus.FORBIDDEN, "Agent is not active");
        }
        URI validatedReturnUrl = returnUrlValidator.validate(agentRegistryEntry, request.returnUrl());

        Set<String> requiredTools = sanitizeTools(request.requiredTools());
        PolicyResolutionResult policyResolutionResult = policyCenterClient.resolveByTools(request.agentId(), requiredTools);
        GatewayAuthContext gatewayAuthContext = gatewayAuthContextStore.find(gatewaySession.gatewaySessionId(), request.agentId())
                .orElse(null);
        if (gatewayAuthContext != null && gatewayAuthContext.covers(policyResolutionResult.requiredPermissionPointCodes(), clock)) {
            long expiresIn = Math.max(0, gatewayAuthContext.expiresAt().getEpochSecond() - clock.instant().getEpochSecond());
            return ResourceTokenResponse.direct(gatewayAuthContext.trToken(), expiresIn);
        }

        String requestId = idGenerator.next("req");
        pendingAuthTransactionStore.save(new PendingAuthTransaction(
                requestId,
                request.agentId(),
                requiredTools,
                policyResolutionResult.requiredPermissionPointCodes(),
                policyResolutionResult.permissionPoints(),
                validatedReturnUrl,
                request.state(),
                gatewaySession.gatewaySessionId(),
                null
        ));

        String redirectUrl = UriComponentsBuilder.fromHttpUrl(properties.getSelfBaseUrl())
                .path("/gw/auth/authorize")
                .queryParam("request_id", requestId)
                .build(true)
                .toUriString();

        return ResourceTokenResponse.redirect(redirectUrl, requestId);
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

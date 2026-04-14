package com.huawei.it.roma.liveeda.auth.service;

import com.huawei.it.roma.liveeda.auth.client.idaas.IdaasAuthorizeSupport;
import com.huawei.it.roma.liveeda.auth.client.idaas.IdaasTokenClient;
import com.huawei.it.roma.liveeda.auth.client.iam.IamAssumeAgentTokenClient;
import com.huawei.it.roma.liveeda.auth.client.iam.IamResourceTokenClient;
import com.huawei.it.roma.liveeda.auth.domain.AgentRegistryEntry;
import com.huawei.it.roma.liveeda.auth.domain.GatewayAuthContext;
import com.huawei.it.roma.liveeda.auth.domain.GatewaySession;
import com.huawei.it.roma.liveeda.auth.domain.IssuedToken;
import com.huawei.it.roma.liveeda.auth.domain.PendingAuthTransaction;
import com.huawei.it.roma.liveeda.auth.domain.PendingBaseLogin;
import com.huawei.it.roma.liveeda.auth.domain.UserAuthorizationResult;
import com.huawei.it.roma.liveeda.auth.store.AgentRegistryStore;
import com.huawei.it.roma.liveeda.auth.store.GatewayAuthContextStore;
import com.huawei.it.roma.liveeda.auth.store.GatewaySessionStore;
import com.huawei.it.roma.liveeda.auth.store.PendingAuthTransactionStore;
import com.huawei.it.roma.liveeda.auth.store.PendingBaseLoginStore;
import com.huawei.it.roma.liveeda.auth.util.IdGenerator;
import com.huawei.it.roma.liveeda.auth.web.GatewayException;
import java.net.URI;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class GatewayAuthService {

    public static final String GATEWAY_SESSION_COOKIE = "gw_session_id";

    private final AgentRegistryStore agentRegistryStore;
    private final PendingBaseLoginStore pendingBaseLoginStore;
    private final PendingAuthTransactionStore pendingAuthTransactionStore;
    private final GatewaySessionStore gatewaySessionStore;
    private final GatewayAuthContextStore gatewayAuthContextStore;
    private final IdaasAuthorizeSupport idaasAuthorizeSupport;
    private final IdaasTokenClient idaasTokenClient;
    private final IamAssumeAgentTokenClient iamAssumeAgentTokenClient;
    private final IamResourceTokenClient iamResourceTokenClient;
    private final ReturnUrlValidator returnUrlValidator;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public URI startBaseLogin(String agentId, String returnUrl, String outerState) {
        AgentRegistryEntry agentRegistryEntry = agentRegistryStore.require(agentId);
        URI validatedReturnUrl = returnUrlValidator.validate(agentRegistryEntry, returnUrl);
        String gwState = idGenerator.next("gw_state");
        pendingBaseLoginStore.save(new PendingBaseLogin(gwState, agentId, validatedReturnUrl, outerState));
        return idaasAuthorizeSupport.buildBaseAuthorizationUri(gwState);
    }

    public BaseLoginCallbackResult handleBaseCallback(String code, String gwState) {
        PendingBaseLogin pendingBaseLogin = pendingBaseLoginStore.find(gwState)
                .orElseThrow(() -> new GatewayException(HttpStatus.UNAUTHORIZED, "Unknown gw_state"));
        pendingBaseLoginStore.delete(gwState);

        var baseLoginResult = idaasTokenClient.exchangeBaseLoginCode(code);
        String gatewaySessionId = idGenerator.next("gws");
        String gatewaySessionToken = idGenerator.next("gwst");

        gatewaySessionStore.save(new GatewaySession(
                gatewaySessionId,
                gatewaySessionToken,
                baseLoginResult.userId(),
                baseLoginResult.username(),
                clock.instant()
        ));

        URI redirectUri = UriComponentsBuilder.fromUri(pendingBaseLogin.returnUrl())
                .queryParam("gw_session_token", gatewaySessionToken)
                .queryParam("user_id", baseLoginResult.userId())
                .queryParam("username", baseLoginResult.username())
                .queryParam("state", pendingBaseLogin.outerState())
                .build(true)
                .toUri();

        return new BaseLoginCallbackResult(gatewaySessionId, redirectUri);
    }

    public URI startConsentAuthorization(String requestId, String gatewaySessionId) {
        PendingAuthTransaction transaction = pendingAuthTransactionStore.findByRequestId(requestId)
                .orElseThrow(() -> new GatewayException(HttpStatus.NOT_FOUND, "Unknown request_id"));
        if (!transaction.gatewaySessionId().equals(gatewaySessionId)) {
            throw new GatewayException(HttpStatus.UNAUTHORIZED, "request_id does not belong to current gateway session");
        }
        String gwState = idGenerator.next("gw_state");
        pendingAuthTransactionStore.save(transaction.withGwState(gwState));
        return idaasAuthorizeSupport.buildConsentAuthorizationUri(gwState, transaction.requiredPolicyCodes());
    }

    public URI handleConsentCallback(String code, String gwState) {
        PendingAuthTransaction transaction = pendingAuthTransactionStore.findByGwState(gwState)
                .orElseThrow(() -> new GatewayException(HttpStatus.UNAUTHORIZED, "Unknown gw_state"));
        pendingAuthTransactionStore.delete(transaction);

        AgentRegistryEntry agentRegistryEntry = agentRegistryStore.require(transaction.agentId());
        UserAuthorizationResult authorizationResult = idaasTokenClient.exchangeConsentCode(code);
        IssuedToken t1 = iamAssumeAgentTokenClient.assumeAgentToken(agentRegistryEntry);
        IssuedToken tr = iamResourceTokenClient.issueResourceToken(agentRegistryEntry, authorizationResult, t1);

        gatewayAuthContextStore.save(new GatewayAuthContext(
                transaction.gatewaySessionId(),
                transaction.agentId(),
                authorizationResult.accessToken(),
                t1.accessToken(),
                tr.accessToken(),
                authorizationResult.consentedPolicyCodes(),
                tr.expiresAt()
        ));

        return UriComponentsBuilder.fromUri(transaction.returnUrl())
                .queryParam("state", transaction.outerState())
                .build(true)
                .toUri();
    }

    public record BaseLoginCallbackResult(String gatewaySessionId, URI redirectUri) {
    }
}

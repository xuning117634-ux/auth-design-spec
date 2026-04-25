package com.huawei.it.roma.liveeda.auth.service;

import com.huawei.it.roma.liveeda.auth.client.idaas.IdaasAuthorizeSupport;
import com.huawei.it.roma.liveeda.auth.client.idaas.IdaasTokenClient;
import com.huawei.it.roma.liveeda.auth.client.iam.IamAssumeAgentTokenClient;
import com.huawei.it.roma.liveeda.auth.client.iam.IamResourceTokenClient;
import com.huawei.it.roma.liveeda.auth.config.AgentGatewayProperties;
import com.huawei.it.roma.liveeda.auth.config.IdaasProperties;
import com.huawei.it.roma.liveeda.auth.domain.AgentRegistryEntry;
import com.huawei.it.roma.liveeda.auth.domain.AuthorizedPermissionPoint;
import com.huawei.it.roma.liveeda.auth.domain.BaseLoginResult;
import com.huawei.it.roma.liveeda.auth.domain.IssuedToken;
import com.huawei.it.roma.liveeda.auth.domain.LoginTicket;
import com.huawei.it.roma.liveeda.auth.domain.PendingAuthTransaction;
import com.huawei.it.roma.liveeda.auth.domain.PendingBaseLogin;
import com.huawei.it.roma.liveeda.auth.domain.TokenResultTicket;
import com.huawei.it.roma.liveeda.auth.domain.UserAuthorizationResult;
import com.huawei.it.roma.liveeda.auth.store.AgentRegistryStore;
import com.huawei.it.roma.liveeda.auth.store.LoginTicketStore;
import com.huawei.it.roma.liveeda.auth.store.PendingAuthTransactionStore;
import com.huawei.it.roma.liveeda.auth.store.PendingBaseLoginStore;
import com.huawei.it.roma.liveeda.auth.store.TokenResultTicketStore;
import com.huawei.it.roma.liveeda.auth.util.IdGenerator;
import com.huawei.it.roma.liveeda.auth.web.GatewayException;
import com.huawei.it.roma.liveeda.auth.web.LoginTicketExchangeRequest;
import com.huawei.it.roma.liveeda.auth.web.LoginTicketExchangeResponse;
import com.huawei.it.roma.liveeda.auth.web.TokenResultExchangeRequest;
import com.huawei.it.roma.liveeda.auth.web.TokenResultExchangeResponse;
import java.net.URI;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class GatewayAuthService {

    private final AgentGatewayProperties properties;
    private final IdaasProperties idaasProperties;
    private final AgentRegistryStore agentRegistryStore;
    private final PendingBaseLoginStore pendingBaseLoginStore;
    private final PendingAuthTransactionStore pendingAuthTransactionStore;
    private final LoginTicketStore loginTicketStore;
    private final TokenResultTicketStore tokenResultTicketStore;
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
        return idaasAuthorizeSupport.buildBaseAuthorizationUri(agentId, gwState);
    }

    public URI handleBaseCallback(String code, String gwState) {
        PendingBaseLogin pendingBaseLogin = pendingBaseLoginStore.find(gwState)
                .orElseThrow(() -> new GatewayException(HttpStatus.UNAUTHORIZED, "Unknown gw_state"));
        pendingBaseLoginStore.delete(gwState);

        String ticketST = idGenerator.next("st");
        loginTicketStore.save(new LoginTicket(
                ticketST,
                pendingBaseLogin.agentId(),
                code,
                idaasProperties.getClientId(),
                properties.getSelfBaseUrl() + "/gw/auth/base/callback",
                pendingBaseLogin.returnUrl(),
                clock.instant()
        ));

        return UriComponentsBuilder.fromUri(pendingBaseLogin.returnUrl())
                .queryParam("ticketST", ticketST)
                .queryParam("state", pendingBaseLogin.outerState())
                .build(true)
                .toUri();
    }

    public LoginTicketExchangeResponse exchangeLoginTicket(LoginTicketExchangeRequest request) {
        LoginTicket ticket = loginTicketStore.find(request.ticketST())
                .orElseThrow(() -> new GatewayException(HttpStatus.UNAUTHORIZED, "ticketST does not exist or has expired"));
        if (!ticket.agentId().equals(request.agentId())) {
            throw new GatewayException(HttpStatus.UNAUTHORIZED, "ticketST does not belong to current agent");
        }

        IssuedToken tc = idaasTokenClient.exchangeAuthorizationCode(ticket.authorizationCode(), ticket.redirectUri());
        BaseLoginResult userInfo = idaasTokenClient.fetchUserInfo(tc.accessToken());
        loginTicketStore.delete(ticket.ticketST());

        return new LoginTicketExchangeResponse(
                new LoginTicketExchangeResponse.UserInfo(userInfo.userId(), userInfo.uuid(), userInfo.username()),
                Math.max(0, tc.expiresAt().getEpochSecond() - clock.instant().getEpochSecond())
        );
    }

    public URI startConsentAuthorization(String requestId) {
        PendingAuthTransaction transaction = pendingAuthTransactionStore.findByRequestId(requestId)
                .orElseThrow(() -> new GatewayException(HttpStatus.NOT_FOUND, "Unknown request_id"));
        String gwState = idGenerator.next("gw_state");
        pendingAuthTransactionStore.save(transaction.withGwState(gwState));
        return idaasAuthorizeSupport.buildConsentAuthorizationUri(
                transaction.agentId(),
                gwState,
                transaction.requiredPermissionPointCodes(),
                transaction.subjectHint()
        );
    }

    public URI handleConsentCallback(String code, String gwState) {
        PendingAuthTransaction transaction = pendingAuthTransactionStore.findByGwState(gwState)
                .orElseThrow(() -> new GatewayException(HttpStatus.UNAUTHORIZED, "Unknown gw_state"));
        pendingAuthTransactionStore.delete(transaction);

        AgentRegistryEntry agentRegistryEntry = agentRegistryStore.require(transaction.agentId());
        IssuedToken tc = idaasTokenClient.exchangeAuthorizationCode(
                code,
                properties.getSelfBaseUrl() + "/gw/auth/consent/callback"
        );
        UserAuthorizationResult authorizationResult = ensureConsentedScopes(
                idaasTokenClient.fetchAuthorizationResult(tc),
                transaction.requiredPermissionPoints()
        );
        IssuedToken t1 = iamAssumeAgentTokenClient.assumeAgentToken(agentRegistryEntry);
        IssuedToken tr = iamResourceTokenClient.issueResourceToken(agentRegistryEntry, authorizationResult, t1);

        String tokenResultTicket = idGenerator.next("trt");
        tokenResultTicketStore.save(new TokenResultTicket(
                tokenResultTicket,
                transaction.requestId(),
                transaction.agentId(),
                tr.accessToken(),
                authorizationResult.userId(),
                authorizationResult.username(),
                authorizationResult.authorizedPermissionPoints(),
                tr.expiresAt(),
                clock.instant()
        ));

        return UriComponentsBuilder.fromUri(transaction.returnUrl())
                .queryParam("token_result_ticket", tokenResultTicket)
                .queryParam("request_id", transaction.requestId())
                .queryParam("state", transaction.outerState())
                .build(true)
                .toUri();
    }

    public TokenResultExchangeResponse exchangeTokenResult(TokenResultExchangeRequest request) {
        TokenResultTicket ticket = tokenResultTicketStore.find(request.tokenResultTicket())
                .orElseThrow(() -> new GatewayException(HttpStatus.UNAUTHORIZED,
                        "token_result_ticket does not exist or has expired"));
        if (!ticket.agentId().equals(request.agentId())) {
            throw new GatewayException(HttpStatus.UNAUTHORIZED, "token_result_ticket does not belong to current agent");
        }
        if (!ticket.requestId().equals(request.requestId())) {
            throw new GatewayException(HttpStatus.UNAUTHORIZED, "token_result_ticket does not belong to request_id");
        }
        tokenResultTicketStore.delete(ticket.tokenResultTicket());

        long expiresIn = Math.max(0, ticket.expiresAt().getEpochSecond() - clock.instant().getEpochSecond());
        return new TokenResultExchangeResponse(
                "TOKEN_READY",
                ticket.requestId(),
                ticket.trToken(),
                expiresIn,
                new TokenResultExchangeResponse.AgencyUser(ticket.userId(), ticket.userId(), ticket.username()),
                ticket.consentedScopes()
        );
    }

    private UserAuthorizationResult ensureConsentedScopes(
            UserAuthorizationResult authorizationResult,
            List<AuthorizedPermissionPoint> fallbackPermissionPoints
    ) {
        if (authorizationResult.authorizedPermissionPoints() != null
                && !authorizationResult.authorizedPermissionPoints().isEmpty()) {
            return authorizationResult;
        }
        return new UserAuthorizationResult(
                authorizationResult.userId(),
                authorizationResult.username(),
                fallbackPermissionPoints.stream().map(AuthorizedPermissionPoint::code)
                        .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new)),
                fallbackPermissionPoints,
                authorizationResult.accessToken(),
                authorizationResult.expiresAt()
        );
    }
}

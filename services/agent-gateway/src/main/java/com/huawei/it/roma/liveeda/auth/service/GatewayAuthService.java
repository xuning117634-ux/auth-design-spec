package com.huawei.it.roma.liveeda.auth.service;

import com.huawei.it.roma.liveeda.auth.client.AgentManagementClient;
import com.huawei.it.roma.liveeda.auth.client.idaas.IdaasAuthorizeSupport;
import com.huawei.it.roma.liveeda.auth.client.idaas.IdaasTokenClient;
import com.huawei.it.roma.liveeda.auth.client.iam.IamAssumeAgentTokenClient;
import com.huawei.it.roma.liveeda.auth.client.iam.IamResourceTokenClient;
import com.huawei.it.roma.liveeda.auth.config.AgentGatewayProperties;
import com.huawei.it.roma.liveeda.auth.config.IdaasProperties;
import com.huawei.it.roma.liveeda.auth.domain.AgentRegistryEntry;
import com.huawei.it.roma.liveeda.auth.domain.BaseLoginResult;
import com.huawei.it.roma.liveeda.auth.domain.IssuedToken;
import com.huawei.it.roma.liveeda.auth.domain.LoginTicket;
import com.huawei.it.roma.liveeda.auth.domain.PendingAuthTransaction;
import com.huawei.it.roma.liveeda.auth.domain.PendingBaseLogin;
import com.huawei.it.roma.liveeda.auth.domain.TokenResultTicket;
import com.huawei.it.roma.liveeda.auth.domain.UserAuthorizationResult;
import com.huawei.it.roma.liveeda.auth.store.LoginTicketStore;
import com.huawei.it.roma.liveeda.auth.store.PendingAuthTransactionStore;
import com.huawei.it.roma.liveeda.auth.store.PendingBaseLoginStore;
import com.huawei.it.roma.liveeda.auth.store.TokenResultTicketStore;
import com.huawei.it.roma.liveeda.auth.util.IdGenerator;
import com.huawei.it.roma.liveeda.auth.util.LogSanitizer;
import com.huawei.it.roma.liveeda.auth.web.GatewayException;
import com.huawei.it.roma.liveeda.auth.web.LoginTicketExchangeRequest;
import com.huawei.it.roma.liveeda.auth.web.LoginTicketExchangeResponse;
import com.huawei.it.roma.liveeda.auth.web.TokenResultExchangeRequest;
import com.huawei.it.roma.liveeda.auth.web.TokenResultExchangeResponse;
import java.net.URI;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Slf4j
public class GatewayAuthService {

    private final AgentGatewayProperties properties;
    private final IdaasProperties idaasProperties;
    private final AgentManagementClient agentManagementClient;
    private final PendingBaseLoginStore pendingBaseLoginStore;
    private final PendingAuthTransactionStore pendingAuthTransactionStore;
    private final LoginTicketStore loginTicketStore;
    private final TokenResultTicketStore tokenResultTicketStore;
    private final IdaasAuthorizeSupport idaasAuthorizeSupport;
    private final IdaasTokenClient idaasTokenClient;
    private final IamAssumeAgentTokenClient iamAssumeAgentTokenClient;
    private final IamResourceTokenClient iamResourceTokenClient;
    private final ReturnUrlValidator returnUrlValidator;
    private final ResourceCookieService resourceCookieService;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public URI startBaseLogin(String agentId, String returnUrl, String outerState) {
        AgentRegistryEntry agentRegistryEntry = agentManagementClient.getGatewayProfile(agentId);
        URI validatedReturnUrl = returnUrlValidator.validate(agentRegistryEntry, returnUrl);
        String gwState = idGenerator.next("gw_state");
        pendingBaseLoginStore.save(new PendingBaseLogin(gwState, agentId, validatedReturnUrl, outerState));
        URI redirectUri = idaasAuthorizeSupport.buildBaseAuthorizationUri(agentId, gwState);
        log.info("base login prepared, agentId={}, appId={}, gwStateTail={}, redirectHost={}",
                agentId, agentRegistryEntry.appId(), LogSanitizer.tail(gwState), LogSanitizer.host(redirectUri));
        return redirectUri;
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

        URI redirectUri = UriComponentsBuilder.fromUri(pendingBaseLogin.returnUrl())
                .queryParam("ticketST", ticketST)
                .queryParam("state", pendingBaseLogin.outerState())
                .build(true)
                .toUri();
        log.info("base callback completed, agentId={}, gwStateTail={}, ticketTail={}, redirectHost={}",
                pendingBaseLogin.agentId(), LogSanitizer.tail(gwState), LogSanitizer.tail(ticketST),
                LogSanitizer.host(redirectUri));
        return redirectUri;
    }

    public LoginTicketExchangeResponse exchangeLoginTicket(LoginTicketExchangeRequest request) {
        LoginTicket ticket = loginTicketStore.find(request.ticketST())
                .orElseThrow(() -> new GatewayException(HttpStatus.UNAUTHORIZED, "ticketST does not exist or has expired"));
        if (!ticket.agentId().equals(request.agentId())) {
            log.warn("login ticket exchange rejected, reason=agent_mismatch, requestAgentId={}, ticketAgentId={}, ticketTail={}",
                    request.agentId(), ticket.agentId(), LogSanitizer.tail(request.ticketST()));
            throw new GatewayException(HttpStatus.UNAUTHORIZED, "ticketST does not belong to current agent");
        }

        IssuedToken tc = idaasTokenClient.exchangeAuthorizationCode(ticket.authorizationCode(), ticket.redirectUri());
        BaseLoginResult userInfo = idaasTokenClient.fetchUserInfo(tc.accessToken());
        loginTicketStore.delete(ticket.ticketST());
        long expiresIn = Math.max(0, tc.expiresAt().getEpochSecond() - clock.instant().getEpochSecond());
        log.info("login ticket exchange completed, agentId={}, ticketTail={}, userId={}, expiresIn={}",
                request.agentId(), LogSanitizer.tail(request.ticketST()), userInfo.userId(), expiresIn);

        return new LoginTicketExchangeResponse(
                new LoginTicketExchangeResponse.UserInfo(userInfo.userId(), userInfo.uuid(), userInfo.username()),
                expiresIn
        );
    }

    public URI startConsentAuthorization(String requestId) {
        PendingAuthTransaction transaction = pendingAuthTransactionStore.findByRequestId(requestId)
                .orElseThrow(() -> new GatewayException(HttpStatus.NOT_FOUND, "Unknown request_id"));
        String gwState = idGenerator.next("gw_state");
        pendingAuthTransactionStore.save(transaction.withGwState(gwState));
        URI redirectUri = idaasAuthorizeSupport.buildConsentAuthorizationUri(
                transaction.agentId(),
                gwState,
                transaction.requiredPermissionPointCodes(),
                transaction.subjectHint()
        );
        log.info("consent authorization prepared, agentId={}, requestId={}, gwStateTail={}, permissionPoints={}, redirectHost={}",
                transaction.agentId(), requestId, LogSanitizer.tail(gwState),
                LogSanitizer.size(transaction.requiredPermissionPointCodes()), LogSanitizer.host(redirectUri));
        return redirectUri;
    }

    public URI handleConsentCallback(String code, String gwState) {
        PendingAuthTransaction transaction = pendingAuthTransactionStore.findByGwState(gwState)
                .orElseThrow(() -> new GatewayException(HttpStatus.UNAUTHORIZED, "Unknown gw_state"));
        pendingAuthTransactionStore.delete(transaction);

        AgentRegistryEntry agentRegistryEntry = agentManagementClient.getGatewayProfile(transaction.agentId());
        IssuedToken tc = idaasTokenClient.exchangeAuthorizationCode(
                code,
                properties.getSelfBaseUrl() + "/gw/auth/consent/callback"
        );
        UserAuthorizationResult authorizationResult = idaasTokenClient.fetchAuthorizationResult(tc);
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

        URI redirectUri = UriComponentsBuilder.fromUri(transaction.returnUrl())
                .queryParam("token_result_ticket", tokenResultTicket)
                .queryParam("request_id", transaction.requestId())
                .queryParam("state", transaction.outerState())
                .build(true)
                .toUri();
        log.info("consent callback completed, agentId={}, requestId={}, userId={}, permissionPoints={}, ticketTail={}, trExpiresAt={}, redirectHost={}",
                transaction.agentId(), transaction.requestId(), authorizationResult.userId(),
                LogSanitizer.size(authorizationResult.authorizedPermissionPointCodes()),
                LogSanitizer.tail(tokenResultTicket), tr.expiresAt(), LogSanitizer.host(redirectUri));
        return redirectUri;
    }

    public TokenResultExchangeResponse exchangeTokenResult(TokenResultExchangeRequest request, String cookieHeader) {
        TokenResultTicket ticket = tokenResultTicketStore.find(request.tokenResultTicket())
                .orElseThrow(() -> new GatewayException(HttpStatus.UNAUTHORIZED,
                        "token_result_ticket does not exist or has expired"));
        if (!ticket.agentId().equals(request.agentId())) {
            log.warn("token result exchange rejected, reason=agent_mismatch, requestAgentId={}, ticketAgentId={}, requestId={}",
                    request.agentId(), ticket.agentId(), request.requestId());
            throw new GatewayException(HttpStatus.UNAUTHORIZED, "token_result_ticket does not belong to current agent");
        }
        if (!ticket.requestId().equals(request.requestId())) {
            log.warn("token result exchange rejected, reason=request_mismatch, agentId={}, requestId={}, ticketRequestId={}",
                    request.agentId(), request.requestId(), ticket.requestId());
            throw new GatewayException(HttpStatus.UNAUTHORIZED, "token_result_ticket does not belong to request_id");
        }
        tokenResultTicketStore.delete(ticket.tokenResultTicket());
        resourceCookieService.cacheCookie(ticket.agentId(), ticket.trToken(), ticket.expiresAt(), cookieHeader);

        long expiresIn = Math.max(0, ticket.expiresAt().getEpochSecond() - clock.instant().getEpochSecond());
        log.info("token result exchange completed, agentId={}, requestId={}, ticketTail={}, cookiePresent={}, expiresIn={}, scopes={}",
                ticket.agentId(), ticket.requestId(), LogSanitizer.tail(ticket.tokenResultTicket()),
                LogSanitizer.present(cookieHeader), expiresIn, LogSanitizer.size(ticket.consentedScopes()));
        return new TokenResultExchangeResponse(
                "TOKEN_READY",
                ticket.requestId(),
                ticket.trToken(),
                expiresIn,
                new TokenResultExchangeResponse.AgencyUser(ticket.userId(), ticket.userId(), ticket.username()),
                ticket.consentedScopes() == null
                        ? List.of()
                        : ticket.consentedScopes().stream()
                                .map(com.huawei.it.roma.liveeda.auth.domain.AuthorizedPermissionPoint::code)
                                .toList()
        );
    }

}

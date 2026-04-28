package com.huawei.it.roma.liveeda.auth.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.huawei.it.roma.liveeda.auth.config.MockTokenProperties;
import com.huawei.it.roma.liveeda.auth.domain.AgentRegistryEntry;
import com.huawei.it.roma.liveeda.auth.domain.AuthorizedPermissionPoint;
import com.huawei.it.roma.liveeda.auth.domain.IssuedToken;
import com.huawei.it.roma.liveeda.auth.domain.UserAuthorizationResult;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("mock")
@RequiredArgsConstructor
public class JwtTokenFactory {

    private static final String MOCK_ENTERPRISE_ID = "11111111111111111111111111111111";
    private static final String MOCK_ACCOUNT_TYPE = "IAM_AI_SERVICE";
    private static final String MOCK_ACCESS_DOMAIN = "middle-secret";

    private final MockTokenProperties properties;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public IssuedToken issueMockTc(String userId, String username, List<AuthorizedPermissionPoint> authorizedPermissionPoints) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(1, ChronoUnit.HOURS);
        String tokenId = idGenerator.next("tc");

        Map<String, Object> userClaim = new LinkedHashMap<>();
        userClaim.put("user_id", userId);
        userClaim.put("username", username);

        String token = JWT.create()
                .withIssuer("mock-idaas")
                .withSubject(userId)
                .withAudience("agent-gateway")
                .withIssuedAt(now)
                .withNotBefore(now)
                .withExpiresAt(expiresAt)
                .withJWTId(tokenId)
                .withClaim("token_id", tokenId)
                .withClaim("user", userClaim)
                .withClaim("consented_scopes", toPermissionPointClaims(authorizedPermissionPoints))
                .sign(algorithm());
        return new IssuedToken(token, expiresAt);
    }

    public IssuedToken issueMockT1(AgentRegistryEntry agentRegistryEntry) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(1, ChronoUnit.DAYS);
        String tokenId = idGenerator.next("t1");
        String delegatorAppId = agentRegistryEntry.appId();
        String delegatorAccountName = "Agent_" + delegatorAppId;

        String token = JWT.create()
                .withIssuer("iam")
                .withSubject(delegatorAppId)
                .withIssuedAt(now)
                .withNotBefore(now)
                .withExpiresAt(expiresAt)
                .withJWTId(tokenId)
                .withClaim("token_id", tokenId)
                .withClaim("name", delegatorAccountName)
                .withClaim("account_id", "mock-account-" + delegatorAccountName)
                .withClaim("enterprise", MOCK_ENTERPRISE_ID)
                .withClaim("account_type", MOCK_ACCOUNT_TYPE)
                .withClaim("project", delegatorAppId)
                .withClaim("access_domain", MOCK_ACCESS_DOMAIN)
                .withClaim("proxy_id", agentRegistryEntry.appId())
                .withClaim("agent", buildAgentClaim(agentRegistryEntry))
                .sign(algorithm());
        return new IssuedToken(token, expiresAt);
    }

    public IssuedToken issueMockTr(AgentRegistryEntry agentRegistryEntry, UserAuthorizationResult authorizationResult) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(1, ChronoUnit.HOURS);
        String tokenId = idGenerator.next("tr");
        String delegatorAppId = agentRegistryEntry.appId();
        String delegatorAccountName = "Agent_" + delegatorAppId;

        Map<String, Object> agencyUserClaim = new LinkedHashMap<>();
        agencyUserClaim.put("idp", "idaas");
        agencyUserClaim.put("idp_id", authorizationResult.userId());
        agencyUserClaim.put("user_id", authorizationResult.userId());
        agencyUserClaim.put("global_user_id", authorizationResult.userId());
        agencyUserClaim.put("ouath_client_id", "agent_gateway_client");
        agencyUserClaim.put("outh_client_app_id", agentRegistryEntry.appId());
        agencyUserClaim.put("consented_scopes", toPermissionPointClaims(authorizationResult.authorizedPermissionPoints()));

        String token = JWT.create()
                .withIssuer("iam")
                .withSubject(delegatorAppId)
                .withIssuedAt(now)
                .withNotBefore(now)
                .withExpiresAt(expiresAt)
                .withJWTId(tokenId)
                .withClaim("token_id", tokenId)
                .withClaim("name", delegatorAccountName)
                .withClaim("account_id", "mock-account-" + delegatorAccountName)
                .withClaim("enterprise", MOCK_ENTERPRISE_ID)
                .withClaim("account_type", MOCK_ACCOUNT_TYPE)
                .withClaim("project", delegatorAppId)
                .withClaim("access_domain", MOCK_ACCESS_DOMAIN)
                .withClaim("agent", buildAgentClaim(agentRegistryEntry))
                .withClaim("agency_user", agencyUserClaim)
                .sign(algorithm());
        return new IssuedToken(token, expiresAt);
    }

    private Algorithm algorithm() {
        return Algorithm.HMAC256(properties.getJwtSecret());
    }

    private Map<String, Object> buildAgentClaim(AgentRegistryEntry agentRegistryEntry) {
        Map<String, Object> agentClaim = new LinkedHashMap<>();
        agentClaim.put("agent_id", agentRegistryEntry.agentId());
        agentClaim.put("agent_name", agentRegistryEntry.agentName());
        agentClaim.put("agent_type", "server");
        return agentClaim;
    }

    private List<Map<String, Object>> toPermissionPointClaims(List<AuthorizedPermissionPoint> permissionPoints) {
        return permissionPoints.stream()
                .map(point -> {
                    Map<String, Object> claim = new LinkedHashMap<>();
                    claim.put("code", point.code());
                    claim.put("displayNameZh", point.displayNameZh());
                    return claim;
                })
                .toList();
    }
}

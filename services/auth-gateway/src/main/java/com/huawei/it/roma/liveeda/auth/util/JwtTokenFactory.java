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
                .withClaim("consented_scopes", toPermissionPointCodes(authorizedPermissionPoints))
                .sign(algorithm());
        return new IssuedToken(token, expiresAt);
    }

    public IssuedToken issueMockT1(AgentRegistryEntry agentRegistryEntry) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(1, ChronoUnit.DAYS);
        String tokenId = idGenerator.next("t1");
        String delegatorAppId = agentRegistryEntry.appId();
        String delegatorAccountName = "Agent_" + agentRegistryEntry.agentId();

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
        String delegatorAccountName = "Agent_" + agentRegistryEntry.agentId();
        List<String> permissionPointCodes = toPermissionPointCodes(authorizationResult.authorizedPermissionPoints());

        Map<String, Object> agencyUserClaim = new LinkedHashMap<>();
        agencyUserClaim.put("idp", "idaas");
        agencyUserClaim.put("idp_id", null);
        agencyUserClaim.put("oauth_client_app_id", null);
        agencyUserClaim.put("user", buildAgencyUserJson(authorizationResult.userId(), authorizationResult.username()));
        agencyUserClaim.put("oauth_client_id", null);
        agencyUserClaim.put("consented_scopes", permissionPointCodes);

        String token = JWT.create()
                .withIssuer("iam")
                .withSubject(delegatorAccountName)
                .withIssuedAt(now)
                .withNotBefore(now)
                .withExpiresAt(expiresAt)
                .withJWTId(tokenId)
                .withClaim("token_id", tokenId)
                .withClaim("aud", agentRegistryEntry.agentId())
                .withClaim("name", delegatorAccountName)
                .withClaim("account_id", "mock-account-" + delegatorAccountName)
                .withClaim("enterprise", MOCK_ENTERPRISE_ID)
                .withClaim("account_type", MOCK_ACCOUNT_TYPE)
                .withClaim("project", delegatorAppId)
                .withClaim("access_domain", MOCK_ACCESS_DOMAIN)
                .withClaim("scope", String.join(" ", permissionPointCodes))
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

    private List<String> toPermissionPointCodes(List<AuthorizedPermissionPoint> permissionPoints) {
        return permissionPoints.stream()
                .map(AuthorizedPermissionPoint::code)
                .toList();
    }

    private String buildAgencyUserJson(String userId, String username) {
        return """
                {"displayName":"%s","displayNameCn":"%s","tenantId":"%s","uid":"%s","uuid":"uuid~%s"}
                """.formatted(
                escapeJson(username),
                escapeJson(username),
                MOCK_ENTERPRISE_ID,
                escapeJson(userId),
                escapeJson(userId)
        ).trim();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}

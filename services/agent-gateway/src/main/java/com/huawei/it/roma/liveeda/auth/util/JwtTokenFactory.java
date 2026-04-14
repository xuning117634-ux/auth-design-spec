package com.huawei.it.roma.liveeda.auth.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.huawei.it.roma.liveeda.auth.config.AgentGatewayProperties;
import com.huawei.it.roma.liveeda.auth.domain.AgentRegistryEntry;
import com.huawei.it.roma.liveeda.auth.domain.IssuedToken;
import com.huawei.it.roma.liveeda.auth.domain.UserAuthorizationResult;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenFactory {

    private final AgentGatewayProperties properties;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public IssuedToken issueMockTc(String userId, String username, Set<String> scopes) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(1, ChronoUnit.HOURS);

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
                .withJWTId(idGenerator.next("tc"))
                .withClaim("user", userClaim)
                .withClaim("consented_scopes", scopes.stream().sorted().toList())
                .sign(algorithm());
        return new IssuedToken(token, expiresAt);
    }

    public IssuedToken issueMockT1(AgentRegistryEntry agentRegistryEntry) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(1, ChronoUnit.DAYS);

        Map<String, Object> agentClaim = new LinkedHashMap<>();
        agentClaim.put("agent_id", agentRegistryEntry.agentId());
        agentClaim.put("agent_name", agentRegistryEntry.agentName());
        agentClaim.put("agent_type", "server");

        String token = JWT.create()
                .withIssuer("mock-iam")
                .withSubject(agentRegistryEntry.principal())
                .withIssuedAt(now)
                .withNotBefore(now)
                .withExpiresAt(expiresAt)
                .withJWTId(idGenerator.next("t1"))
                .withClaim("agent", agentClaim)
                .withClaim("name", agentRegistryEntry.agentServiceAccount())
                .sign(algorithm());
        return new IssuedToken(token, expiresAt);
    }

    public IssuedToken issueMockTr(AgentRegistryEntry agentRegistryEntry, UserAuthorizationResult authorizationResult) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(1, ChronoUnit.HOURS);

        Map<String, Object> agentClaim = new LinkedHashMap<>();
        agentClaim.put("agent_id", agentRegistryEntry.agentId());
        agentClaim.put("agent_name", agentRegistryEntry.agentName());
        agentClaim.put("agent_type", "server");

        Map<String, Object> agencyUserClaim = new LinkedHashMap<>();
        agencyUserClaim.put("idp", "mock-idaas");
        agencyUserClaim.put("user_id", authorizationResult.userId());
        agencyUserClaim.put("global_user_id", authorizationResult.userId());
        agencyUserClaim.put("oauth_client_id", "agent_gateway_client");
        agencyUserClaim.put("oauth_client_app_id", agentRegistryEntry.principal());
        agencyUserClaim.put("consented_scopes", authorizationResult.consentedPolicyCodes().stream().sorted().toList());

        String token = JWT.create()
                .withIssuer("mock-iam")
                .withSubject(agentRegistryEntry.principal())
                .withIssuedAt(now)
                .withNotBefore(now)
                .withExpiresAt(expiresAt)
                .withJWTId(idGenerator.next("tr"))
                .withClaim("agent", agentClaim)
                .withClaim("agency_user", agencyUserClaim)
                .sign(algorithm());
        return new IssuedToken(token, expiresAt);
    }

    private Algorithm algorithm() {
        return Algorithm.HMAC256(properties.getJwtSecret());
    }
}

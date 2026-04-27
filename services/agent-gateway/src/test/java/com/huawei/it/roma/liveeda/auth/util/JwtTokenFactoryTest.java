package com.huawei.it.roma.liveeda.auth.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.huawei.it.roma.liveeda.auth.config.AgentGatewayProperties;
import com.huawei.it.roma.liveeda.auth.domain.AgentRegistryEntry;
import com.huawei.it.roma.liveeda.auth.domain.AuthorizedPermissionPoint;
import com.huawei.it.roma.liveeda.auth.domain.UserAuthorizationResult;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JwtTokenFactoryTest {

    @Test
    void shouldWriteConsentedScopesIntoMockTc() {
        JwtTokenFactory jwtTokenFactory = buildFactory();

        String token = jwtTokenFactory.issueMockTc(
                        "z01062668",
                        "demo.user",
                        List.of(new AuthorizedPermissionPoint("erp:contract:r", "ERP 合同的可读权限")))
                .accessToken();

        DecodedJWT decodedJWT = JWT.decode(token);
        assertEquals("erp:contract:r", decodedJWT.getClaim("consented_scopes").asList(Object.class).stream()
                .map(value -> (java.util.Map<?, ?>) value)
                .map(value -> String.valueOf(value.get("code")))
                .findFirst()
                .orElseThrow());
    }

    @Test
    void shouldWriteConsentedScopesIntoMockTrAgencyUser() {
        JwtTokenFactory jwtTokenFactory = buildFactory();

        String token = jwtTokenFactory.issueMockTr(
                        new AgentRegistryEntry(
                                "agt_business_001",
                                "业务数据助手",
                                "ent_001",
                                "com.huawei.business.agent",
                                List.of("localhost"),
                                Set.of("erp:contract:r")
                        ),
                        new UserAuthorizationResult(
                                "z01062668",
                                "demo.user",
                                Set.of("erp:contract:r"),
                                List.of(new AuthorizedPermissionPoint("erp:contract:r", "ERP 合同的可读权限")),
                                "tc_demo_001",
                                Instant.parse("2026-04-21T10:00:00Z")
                        ))
                .accessToken();

        DecodedJWT decodedJWT = JWT.decode(token);
        java.util.Map<String, Object> agencyUser = decodedJWT.getClaim("agency_user").asMap();
        assertTrue(agencyUser.containsKey("consented_scopes"));
        @SuppressWarnings("unchecked")
        List<java.util.Map<String, Object>> consentedScopes = (List<java.util.Map<String, Object>>) agencyUser.get("consented_scopes");
        assertEquals("erp:contract:r", consentedScopes.getFirst().get("code"));
        assertEquals("ERP 合同的可读权限", consentedScopes.getFirst().get("displayNameZh"));
    }

    private JwtTokenFactory buildFactory() {
        AgentGatewayProperties gatewayProperties = new AgentGatewayProperties();
        gatewayProperties.setJwtSecret("changeit-changeit-changeit");
        gatewayProperties.setSelfBaseUrl("http://localhost:18080");
        gatewayProperties.setDefaultUserId("z01062668");
        gatewayProperties.setDefaultUsername("demo.user");

        Clock fixedClock = Clock.fixed(Instant.parse("2026-04-21T10:00:00Z"), ZoneOffset.UTC);
        return new JwtTokenFactory(gatewayProperties, fixedClock, new IdGenerator());
    }
}

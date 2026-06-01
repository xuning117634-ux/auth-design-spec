package com.huawei.it.roma.liveeda.auth.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.it.roma.liveeda.auth.config.MockTokenProperties;
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
                        List.of(new AuthorizedPermissionPoint("erp:contract:r", "ERP contract read permission")))
                .accessToken();

        DecodedJWT decodedJWT = JWT.decode(token);
        assertEquals("erp:contract:r", decodedJWT.getClaim("consented_scopes").asList(String.class).getFirst());
    }

    @Test
    void shouldWriteRealIamLikeConsentedScopesIntoMockTrAgencyUser() throws Exception {
        JwtTokenFactory jwtTokenFactory = buildFactory();

        String token = jwtTokenFactory.issueMockTr(
                        new AgentRegistryEntry(
                                "agt_business_001",
                                "business-data-assistant",
                                "ent_001",
                                "com.huawei.business.agent",
                                List.of("localhost"),
                                Set.of("erp:contract:r")
                        ),
                        new UserAuthorizationResult(
                                "z01062668",
                                "demo.user",
                                Set.of("erp:contract:r"),
                                List.of(new AuthorizedPermissionPoint("erp:contract:r", "ERP contract read permission")),
                                "tc_demo_001",
                                Instant.parse("2026-04-21T10:00:00Z")
                        ))
                .accessToken();

        DecodedJWT decodedJWT = JWT.decode(token);
        assertEquals("Agent_agt_business_001", decodedJWT.getClaim("name").asString());
        assertEquals("Agent_agt_business_001", decodedJWT.getSubject());
        assertEquals("agt_business_001", decodedJWT.getClaim("aud").asString());
        assertEquals("erp:contract:r", decodedJWT.getClaim("scope").asString());
        java.util.Map<String, Object> agencyUser = decodedJWT.getClaim("agency_user").asMap();
        assertTrue(agencyUser.containsKey("consented_scopes"));
        @SuppressWarnings("unchecked")
        List<String> consentedScopes = (List<String>) agencyUser.get("consented_scopes");
        assertEquals("erp:contract:r", consentedScopes.getFirst());
        assertEquals("z01062668", new ObjectMapper()
                .readTree(String.valueOf(agencyUser.get("user")))
                .path("uid")
                .asText());
    }

    private JwtTokenFactory buildFactory() {
        MockTokenProperties mockTokenProperties = new MockTokenProperties();
        mockTokenProperties.setJwtSecret("changeit-changeit-changeit");

        Clock fixedClock = Clock.fixed(Instant.parse("2026-04-21T10:00:00Z"), ZoneOffset.UTC);
        return new JwtTokenFactory(mockTokenProperties, fixedClock, new IdGenerator());
    }
}

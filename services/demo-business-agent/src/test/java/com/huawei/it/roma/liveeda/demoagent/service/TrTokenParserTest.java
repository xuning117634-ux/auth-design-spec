package com.huawei.it.roma.liveeda.demoagent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TrTokenParserTest {

    private final TrTokenParser parser = new TrTokenParser(new ObjectMapper());

    @Test
    void shouldReadAgentUserAndPermissionPointsFromRealIamPayloadShape() {
        TrTokenParser.DecodedTrContext context = parser.decode(buildTr(
                "agt_business_001",
                "Y30037812",
                "ignored:scope:r",
                "erp:report:read",
                "erp:invoice:read"
        ));

        assertEquals("agt_business_001", context.agentId());
        assertEquals("Y30037812", context.userId());
        assertEquals(Set.of("erp:report:read", "erp:invoice:read"), context.authorizedPermissionPointCodes());
    }

    @Test
    void shouldRejectObjectConsentedScopes() {
        String header = base64("{\"alg\":\"none\",\"typ\":\"JWT\"}");
        String payload = base64("""
                {
                  "aud": "agt_business_001",
                  "agency_user": {
                    "user": "{\\"uid\\":\\"Y30037812\\"}",
                    "consented_scopes": [{"code":"erp:report:read"}]
                  }
                }
                """);

        assertThrows(IllegalArgumentException.class, () -> parser.decode(header + "." + payload + "."));
    }

    private String buildTr(String agentId, String userId, String scope, String... permissionPointCodes) {
        String header = base64("{\"alg\":\"none\",\"typ\":\"JWT\"}");
        String payload = base64(("""
                {
                  "aud": "%s",
                  "scope": "%s",
                  "agency_user": {
                    "user": "{\\"uid\\":\\"%s\\"}",
                    "consented_scopes": [%s]
                  }
                }
                """).formatted(
                agentId,
                scope,
                userId,
                String.join(",", List.of(permissionPointCodes).stream()
                        .map(code -> "\"" + code + "\"")
                        .toList())
        ));
        return header + "." + payload + ".";
    }

    private String base64(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}

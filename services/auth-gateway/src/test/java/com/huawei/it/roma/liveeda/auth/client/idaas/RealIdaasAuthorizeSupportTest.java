package com.huawei.it.roma.liveeda.auth.client.idaas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.huawei.it.roma.liveeda.auth.config.AgentGatewayProperties;
import com.huawei.it.roma.liveeda.auth.config.IdaasProperties;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

class RealIdaasAuthorizeSupportTest {

    @Test
    void shouldUseBaseProfileScopeForBaseLogin() {
        RealIdaasAuthorizeSupport support = new RealIdaasAuthorizeSupport(gatewayProperties(), idaasProperties());

        URI uri = support.buildBaseAuthorizationUri("agt_business_001", "gw_state_001");

        Map<String, String> params = queryParams(uri);
        assertEquals("https://idaas.example.com/saaslogin1/oauth2/agent/authorize", uriWithoutQuery(uri));
        assertEquals("code", params.get("response_type"));
        assertEquals("agent_gateway_client", params.get("client_id"));
        assertEquals("agt_business_001", params.get("agent_id"));
        assertEquals("base.profile", params.get("scope"));
        assertEquals("gw_state_001", params.get("state"));
        assertEquals("http://localhost:18080/gw/auth/base/callback", params.get("redirect_uri"));
    }

    @Test
    void shouldJoinConsentScopesWithSpace() {
        RealIdaasAuthorizeSupport support = new RealIdaasAuthorizeSupport(gatewayProperties(), idaasProperties());

        URI uri = support.buildConsentAuthorizationUri(
                "agt_business_001",
                "gw_state_002",
                Set.of("erp:invoice:r", "erp:contract:r"),
                Map.of("userId", "Y30037812")
        );

        Map<String, String> params = queryParams(uri);
        assertEquals("erp:contract:r erp:invoice:r", params.get("scope"));
        assertEquals("Y30037812", params.get("login_hint"));
        assertEquals("http://localhost:18080/gw/auth/consent/callback", params.get("redirect_uri"));
    }

    private AgentGatewayProperties gatewayProperties() {
        AgentGatewayProperties properties = new AgentGatewayProperties();
        properties.setSelfBaseUrl("http://localhost:18080");
        return properties;
    }

    private IdaasProperties idaasProperties() {
        IdaasProperties properties = new IdaasProperties();
        properties.setAuthorizeUrl("https://idaas.example.com/saaslogin1/oauth2/agent/authorize");
        properties.setTokenUrl("https://idaas.example.com/saaslogin1/oauth2/agent/token");
        properties.setUserinfoUrl("https://idaas.example.com/saaslogin1/oauth2/agent/userinfo");
        properties.setClientId("agent_gateway_client");
        properties.setClientSecret("mock-client-secret");
        return properties;
    }

    private Map<String, String> queryParams(URI uri) {
        return UriComponentsBuilder.fromUri(uri)
                .build()
                .getQueryParams()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> URLDecoder.decode(entry.getValue().getFirst(), StandardCharsets.UTF_8)));
    }

    private String uriWithoutQuery(URI uri) {
        return UriComponentsBuilder.fromUri(uri).replaceQuery(null).build().toUriString();
    }
}

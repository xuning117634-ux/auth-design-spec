package com.huawei.it.roma.liveeda.auth.client.idaas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.huawei.it.roma.liveeda.auth.config.IdaasProperties;
import com.huawei.it.roma.liveeda.auth.domain.BaseLoginResult;
import com.huawei.it.roma.liveeda.auth.domain.IssuedToken;
import com.huawei.it.roma.liveeda.auth.domain.UserAuthorizationResult;
import com.huawei.it.roma.liveeda.auth.web.GatewayException;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

class RealIdaasTokenClientTest {

    @Test
    void shouldExchangeCodeWithFormUrlencodedRequest() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RealIdaasTokenClient client = new RealIdaasTokenClient(builder, idaasProperties());

        MultiValueMap<String, String> expectedForm = new LinkedMultiValueMap<>();
        expectedForm.add("grant_type", "authorization_code");
        expectedForm.add("code", "code_001");
        expectedForm.add("redirect_uri", "http://localhost:18080/gw/auth/base/callback");

        server.expect(once(), requestTo("https://idaas.example.com/saaslogin1/oauth2/agent/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                .andExpect(header(HttpHeaders.AUTHORIZATION, basicAuth("agent_gateway_client", "mock-client-secret")))
                .andExpect(content().formData(expectedForm))
                .andRespond(withSuccess("""
                        {
                          "access_token": "opaque_tc_token",
                          "token_type": "Bearer",
                          "expires_in": 3600,
                          "refresh_token": "refresh_001"
                        }
                        """, MediaType.APPLICATION_JSON));

        IssuedToken token = client.exchangeAuthorizationCode("code_001", "http://localhost:18080/gw/auth/base/callback");

        assertEquals("opaque_tc_token", token.accessToken());
        server.verify();
    }

    private String basicAuth(String clientId, String clientSecret) {
        String credential = clientId + ":" + clientSecret;
        return "Basic " + Base64.getEncoder().encodeToString(credential.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void shouldMapFormalUserinfoResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RealIdaasTokenClient client = new RealIdaasTokenClient(builder, idaasProperties());

        expectUserInfo(server, "tc_userinfo_token");

        BaseLoginResult result = client.fetchUserInfo("tc_userinfo_token");

        assertEquals("Y30037812", result.userId());
        assertEquals("uuid~eTMwMDM3ODEy", result.uuid());
        assertEquals("Demo User", result.username());
        server.verify();
    }

    @Test
    void shouldParseConsentedScopesFromTcJwt() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RealIdaasTokenClient client = new RealIdaasTokenClient(builder, idaasProperties());
        String tc = tcJwt(List.of("erp:contract:r"));

        expectUserInfo(server, tc);

        UserAuthorizationResult result = client.fetchAuthorizationResult(new IssuedToken(
                tc,
                Instant.parse("2026-04-27T10:00:00Z")
        ));

        assertEquals("Y30037812", result.userId());
        assertEquals("erp:contract:r", result.authorizedPermissionPoints().getFirst().code());
        assertEquals("erp:contract:r", result.authorizedPermissionPoints().getFirst().displayNameZh());
        server.verify();
    }

    @Test
    void shouldRejectTcWithoutConsentedScopes() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RealIdaasTokenClient client = new RealIdaasTokenClient(builder, idaasProperties());
        String tc = tcJwt(List.of());

        expectUserInfo(server, tc);

        assertThrows(GatewayException.class, () -> client.fetchAuthorizationResult(new IssuedToken(
                tc,
                Instant.parse("2026-04-27T10:00:00Z")
        )));
        server.verify();
    }

    private void expectUserInfo(MockRestServiceServer server, String accessToken) {
        server.expect(once(), requestTo("https://idaas.example.com/saaslogin1/oauth2/agent/userinfo"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andRespond(withSuccess("""
                        {
                          "cn": "yangchengyi 30037812",
                          "displayName": "Demo User",
                          "displayNameCn": "Demo User",
                          "displayNameEn": "yangchengyi",
                          "email": "yangchengyi1@huawei.com",
                          "employeeNumber": "30037812",
                          "employeeType": "WX",
                          "givenName": "yangchengyi",
                          "globalUserID": "182641443806804",
                          "sn": "30037812",
                          "telephoneNumber": "008615627558168",
                          "tenantId": "11111111111111111111111111111111",
                          "uid": "Y30037812",
                          "uuid": "uuid~eTMwMDM3ODEy"
                        }
                        """, MediaType.APPLICATION_JSON));
    }

    private String tcJwt(List<String> consentedScopes) {
        return JWT.create()
                .withIssuer("idaas")
                .withSubject("Y30037812")
                .withExpiresAt(Instant.parse("2026-04-27T10:00:00Z"))
                .withClaim("consented_scopes", consentedScopes)
                .sign(Algorithm.HMAC256("test-secret"));
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
}

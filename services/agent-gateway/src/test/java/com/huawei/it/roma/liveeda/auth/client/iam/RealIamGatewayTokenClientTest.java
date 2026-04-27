package com.huawei.it.roma.liveeda.auth.client.iam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.huawei.it.roma.liveeda.auth.config.IamProperties;
import com.huawei.it.roma.liveeda.auth.domain.IssuedToken;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RealIamGatewayTokenClientTest {

    @Test
    void shouldRequestGatewayAgentTokenAndReuseCachedToken() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RealIamGatewayTokenClient client = new RealIamGatewayTokenClient(
                builder,
                iamProperties(),
                Clock.fixed(Instant.parse("2026-04-27T00:00:00Z"), ZoneOffset.UTC)
        );

        server.expect(once(), requestTo("https://iam.example.com/iam/auth/agent-token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json("""
                        {
                          "data": {
                            "type": "agent_token",
                            "attributes": {
                              "account": "gateway-ai-account",
                              "secret": "gateway-ai-secret",
                              "project": "com.huawei.gateway",
                              "enterprise": "ent_001"
                            }
                          }
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "message": "OK",
                          "code": "201",
                          "enterprise": "ent_001",
                          "access_token": "gateway_agent_token",
                          "expires_on": 1777347029000,
                          "token_type": "IAM_AI_SERVICE"
                        }
                        """, MediaType.APPLICATION_JSON));

        IssuedToken first = client.getGatewayAgentToken();
        IssuedToken second = client.getGatewayAgentToken();

        assertEquals("gateway_agent_token", first.accessToken());
        assertEquals(first, second);
        server.verify();
    }

    private IamProperties iamProperties() {
        IamProperties properties = new IamProperties();
        properties.setBaseUrl("https://iam.example.com");
        properties.setAgentTokenPath("/iam/auth/agent-token");
        properties.setAssumeAgentTokenPath("/iam/projects/{gatewayProject}/assume-agent-token");
        properties.setResourceTokenPath("/iam/auth/resource-token");
        properties.setGatewayAccount("gateway-ai-account");
        properties.setGatewaySecret("gateway-ai-secret");
        properties.setGatewayProject("com.huawei.gateway");
        properties.setGatewayEnterprise("ent_001");
        properties.setGatewayTokenRefreshSkewSeconds(300);
        return properties;
    }
}

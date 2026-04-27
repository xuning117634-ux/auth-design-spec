package com.huawei.it.roma.liveeda.auth.client.iam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.huawei.it.roma.liveeda.auth.config.IamProperties;
import com.huawei.it.roma.liveeda.auth.domain.AgentRegistryEntry;
import com.huawei.it.roma.liveeda.auth.domain.IssuedToken;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RealIamAssumeAgentTokenClientTest {

    @Test
    void shouldAssumeBusinessAgentTokenWithGatewayToken() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RealIamAssumeAgentTokenClient client = new RealIamAssumeAgentTokenClient(
                builder,
                iamProperties(),
                () -> new IssuedToken("gateway_agent_token", Instant.parse("2026-04-28T00:00:00Z"))
        );

        server.expect(once(), requestTo("https://iam.example.com/iam/projects/com.huawei.gateway/assume-agent-token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "gateway_agent_token"))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json("""
                        {
                          "data": {
                            "type": "assume_agent_token",
                            "attributes": {
                              "delegator_account_name": "Agent_com.huawei.business.agent",
                              "delegator_appid": "com.huawei.business.agent"
                            }
                          }
                        }
                        """))
                .andExpect(content().string(not(containsString("agent_id"))))
                .andRespond(withSuccess("""
                        {
                          "message": "OK",
                          "code": "201",
                          "enterprise": "ent_001",
                          "access_token": "business_t1_token",
                          "expires_on": 1777347029000,
                          "token_type": "IAM_AI_SERVICE"
                        }
                        """, MediaType.APPLICATION_JSON));

        IssuedToken t1 = client.assumeAgentToken(agentRegistryEntry());

        assertEquals("business_t1_token", t1.accessToken());
        server.verify();
    }

    private AgentRegistryEntry agentRegistryEntry() {
        return new AgentRegistryEntry(
                "agt_business_001",
                "business-data-assistant",
                "ent_001",
                "com.huawei.business.agent",
                List.of("localhost"),
                Set.of("erp:contract:r")
        );
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

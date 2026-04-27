package com.huawei.it.roma.liveeda.auth.client.iam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.huawei.it.roma.liveeda.auth.config.IamProperties;
import com.huawei.it.roma.liveeda.auth.domain.AgentRegistryEntry;
import com.huawei.it.roma.liveeda.auth.domain.AuthorizedPermissionPoint;
import com.huawei.it.roma.liveeda.auth.domain.IssuedToken;
import com.huawei.it.roma.liveeda.auth.domain.UserAuthorizationResult;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RealIamResourceTokenClientTest {

    @Test
    void shouldIssueResourceTokenWithDirectT1AuthorizationHeader() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RealIamResourceTokenClient client = new RealIamResourceTokenClient(builder, iamProperties());

        server.expect(once(), requestTo("https://iam.example.com/iam/auth/resource-token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "business_t1_token"))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json("""
                        {
                          "data": {
                            "type": "resource_token",
                            "attributes": {
                              "user_token": "tc_access_token"
                            }
                          }
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "message": "OK",
                          "code": "201",
                          "enterprise": "ent_001",
                          "access_token": "tr_access_token",
                          "expires_on": 1777347397000,
                          "token_type": "IAM_AI_SERVICE"
                        }
                        """, MediaType.APPLICATION_JSON));

        IssuedToken tr = client.issueResourceToken(
                agentRegistryEntry(),
                userAuthorizationResult(),
                new IssuedToken("business_t1_token", Instant.parse("2026-04-28T00:00:00Z"))
        );

        assertEquals("tr_access_token", tr.accessToken());
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

    private UserAuthorizationResult userAuthorizationResult() {
        return new UserAuthorizationResult(
                "Y30037812",
                "Demo User",
                Set.of("erp:contract:r"),
                List.of(new AuthorizedPermissionPoint("erp:contract:r", "ERP contract read permission")),
                "tc_access_token",
                Instant.parse("2026-04-28T00:00:00Z")
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

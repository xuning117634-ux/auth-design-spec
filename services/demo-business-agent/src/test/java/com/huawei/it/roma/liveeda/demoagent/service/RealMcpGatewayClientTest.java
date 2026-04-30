package com.huawei.it.roma.liveeda.demoagent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.it.roma.liveeda.demoagent.client.PolicyCenterClient;
import com.huawei.it.roma.liveeda.demoagent.config.DemoAgentProperties;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RealMcpGatewayClientTest {

    @Test
    void shouldInvokeConfiguredMcpGatewayWithTrAndHeaders() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RealMcpGatewayClient client = new RealMcpGatewayClient(
                builder,
                properties(),
                new ObjectMapper(),
                org.mockito.Mockito.mock(PolicyCenterClient.class)
        );
        String tr = buildTr("agt_business_001", "Y30037812", "erp:contract:r");

        server.expect(once(), requestTo("https://apig-beta.his.huawei.com/api/dev/mcp-gateway/internal/v1/tools/invoke"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-HW-ID", "com.huawei.pass.roma.event"))
                .andExpect(header("X-HW-APPKEY", "mock-app-key"))
                .andExpect(jsonPath("$.agentId").value("agt_business_001"))
                .andExpect(jsonPath("$.tr").value(tr))
                .andExpect(jsonPath("$.tools[0]").value("mcp:contract-server/get_contract"))
                .andExpect(jsonPath("$.message").value("show contract"))
                .andRespond(withSuccess("""
                        {
                          "status": "SUCCESS",
                          "answer": "real mcp contract result"
                        }
                        """, MediaType.APPLICATION_JSON));

        String answer = client.invoke(
                "agt_business_001",
                tr,
                Set.of("mcp:contract-server/get_contract"),
                "show contract"
        );

        assertEquals("real mcp contract result", answer);
        server.verify();
    }

    private DemoAgentProperties properties() {
        DemoAgentProperties properties = new DemoAgentProperties();
        properties.setGatewayBaseUrl("http://localhost:18080");
        properties.setPolicyCenterBaseUrl("http://localhost:18081");
        properties.setSelfBaseUrl("http://localhost:18082");
        properties.setAgentId("agt_business_001");
        properties.getMcp().setGatewayBaseUrl("https://apig-beta.his.huawei.com/api/dev/mcp-gateway");
        properties.getMcp().setInvokePath("/internal/v1/tools/invoke");
        properties.getMcp().setHeaders(Map.of(
                "X-HW-ID", "com.huawei.pass.roma.event",
                "X-HW-APPKEY", "mock-app-key"
        ));
        return properties;
    }

    private String buildTr(String agentId, String userId, String... permissionPointCodes) {
        String header = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(("""
                        {
                          "aud": "%s",
                          "agency_user": {
                            "user": "{\\"uid\\":\\"%s\\"}",
                            "consented_scopes": [%s]
                          }
                        }
                        """.formatted(
                        agentId,
                        userId,
                        String.join(",", List.of(permissionPointCodes).stream()
                                .map(code -> "\"" + code + "\"")
                                .toList())
                )).getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".";
    }
}

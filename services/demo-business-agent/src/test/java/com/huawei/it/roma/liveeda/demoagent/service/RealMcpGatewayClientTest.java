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

        server.expect(once(), requestTo("https://apig-beta.his.huawei.com/api/dev/mcp-gateway/internal/v1/tools/invoke"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-HW-ID", "com.huawei.pass.roma.event"))
                .andExpect(header("X-HW-APPKEY", "mock-app-key"))
                .andExpect(jsonPath("$.agentId").value("agt_business_001"))
                .andExpect(jsonPath("$.tr").value("tr_demo_001"))
                .andExpect(jsonPath("$.tools[0]").value("mcp:contract-server/get_contract"))
                .andExpect(jsonPath("$.message").value("帮我看一下ERP合同"))
                .andRespond(withSuccess("""
                        {
                          "status": "SUCCESS",
                          "answer": "真实 MCP 返回合同信息"
                        }
                        """, MediaType.APPLICATION_JSON));

        String answer = client.invoke(
                "agt_business_001",
                "tr_demo_001",
                Set.of("mcp:contract-server/get_contract"),
                "帮我看一下ERP合同"
        );

        assertEquals("真实 MCP 返回合同信息", answer);
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
}

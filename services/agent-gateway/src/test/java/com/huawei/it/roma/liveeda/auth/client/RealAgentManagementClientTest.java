package com.huawei.it.roma.liveeda.auth.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.huawei.it.roma.liveeda.auth.config.AgentManagementClientProperties;
import com.huawei.it.roma.liveeda.auth.domain.AgentRegistryEntry;
import com.huawei.it.roma.liveeda.auth.web.GatewayException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RealAgentManagementClientTest {

    @Test
    void shouldQueryAgentMallByAgentIdWithConfiguredHeaders() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RealAgentManagementClient client = new RealAgentManagementClient(builder, properties());

        server.expect(once(), requestTo("https://apig-beta.his.huawei.com/api/dev/public/agentMall/queryByAgentId?agentId=agent_001"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("agentId", "agent_001"))
                .andExpect(header("X-HW-ID", "com.huawei.pass.roma.event"))
                .andExpect(header("X-HW-APPKEY", "mock-app-key"))
                .andRespond(withSuccess("""
                        {
                          "status": "0000",
                          "message": "Success",
                          "data": {
                            "name": "测试Agent",
                            "description": "测试Agent",
                            "subscriptionPermissionPoints": ["event:eventDetail:r"],
                            "policy": "[]",
                            "allowedReturnHosts": ["localhost"],
                            "uniqueId": "agent_001",
                            "appId": "com.huawei.pass.roma.event",
                            "enterpriseId": "11111111111111111111111111111111"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        AgentRegistryEntry entry = client.getGatewayProfile("agent_001");

        assertEquals("agent_001", entry.agentId());
        assertEquals("测试Agent", entry.agentName());
        assertEquals("11111111111111111111111111111111", entry.enterprise());
        assertEquals("com.huawei.pass.roma.event", entry.appId());
        assertEquals("localhost", entry.allowedReturnHosts().getFirst());
        assertEquals("event:eventDetail:r", entry.subscribedPermissionPointCodes().iterator().next());
        server.verify();
    }

    @Test
    void shouldRejectMismatchedUniqueId() {
        RealAgentManagementClient client = clientResponding("""
                {
                  "status": "0000",
                  "message": "Success",
                  "data": {
                    "name": "测试Agent",
                    "subscriptionPermissionPoints": ["event:eventDetail:r"],
                    "allowedReturnHosts": ["localhost"],
                    "uniqueId": "agent_other",
                    "appId": "com.huawei.pass.roma.event",
                    "enterpriseId": "11111111111111111111111111111111"
                  }
                }
                """);

        assertThrows(GatewayException.class, () -> client.getGatewayProfile("agent_001"));
    }

    @Test
    void shouldRejectNonSuccessStatus() {
        RealAgentManagementClient client = clientResponding("""
                {
                  "status": "1001",
                  "message": "Agent not found",
                  "data": null
                }
                """);

        assertThrows(GatewayException.class, () -> client.getGatewayProfile("agent_001"));
    }

    @Test
    void shouldRejectMissingRequiredRuntimeFields() {
        RealAgentManagementClient client = clientResponding("""
                {
                  "status": "0000",
                  "message": "Success",
                  "data": {
                    "name": "测试Agent",
                    "uniqueId": "agent_001",
                    "appId": "com.huawei.pass.roma.event",
                    "enterpriseId": "11111111111111111111111111111111"
                  }
                }
                """);

        assertThrows(GatewayException.class, () -> client.getGatewayProfile("agent_001"));
    }

    private RealAgentManagementClient clientResponding(String body) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://apig-beta.his.huawei.com/api/dev/public/agentMall/queryByAgentId?agentId=agent_001"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        return new RealAgentManagementClient(builder, properties());
    }

    private AgentManagementClientProperties properties() {
        AgentManagementClientProperties properties = new AgentManagementClientProperties();
        properties.setBaseUrl("https://apig-beta.his.huawei.com");
        properties.setQueryByAgentIdPath("/api/dev/public/agentMall/queryByAgentId");
        properties.setHeaders(Map.of(
                "X-HW-ID", "com.huawei.pass.roma.event",
                "X-HW-APPKEY", "mock-app-key"
        ));
        return properties;
    }
}

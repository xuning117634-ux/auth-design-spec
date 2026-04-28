package com.huawei.it.roma.liveeda.demoagent.service;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.profiles.active=real")
class RealProfileDefaultMcpModeTest {

    @Autowired
    private McpGatewayClient mcpGatewayClient;

    @Test
    void shouldKeepMockMcpInRealProfileByDefault() {
        assertInstanceOf(MockMcpGatewayClient.class, mcpGatewayClient);
    }
}

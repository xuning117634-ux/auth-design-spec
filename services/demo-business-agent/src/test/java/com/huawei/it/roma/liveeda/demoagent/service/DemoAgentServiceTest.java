package com.huawei.it.roma.liveeda.demoagent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huawei.it.roma.liveeda.demoagent.client.AgentGatewayClient;
import com.huawei.it.roma.liveeda.demoagent.client.GatewayTokenResponse;
import com.huawei.it.roma.liveeda.demoagent.config.DemoAgentProperties;
import com.huawei.it.roma.liveeda.demoagent.domain.SiteSession;
import com.huawei.it.roma.liveeda.demoagent.store.SiteSessionStore;
import com.huawei.it.roma.liveeda.demoagent.store.TrCacheStore;
import com.huawei.it.roma.liveeda.demoagent.util.IdGenerator;
import com.huawei.it.roma.liveeda.demoagent.web.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DemoAgentServiceTest {

    @Mock
    private AgentGatewayClient agentGatewayClient;

    @Mock
    private MockMcpGatewayClient mockMcpGatewayClient;

    private DemoAgentService demoAgentService;
    private SiteSession siteSession;

    @BeforeEach
    void setUp() {
        DemoAgentProperties properties = new DemoAgentProperties();
        properties.setAgentId("agt_business_001");
        properties.setGatewayBaseUrl("http://localhost:18080");
        properties.setPolicyCenterBaseUrl("http://localhost:18081");
        properties.setSelfBaseUrl("http://localhost:18082");

        demoAgentService = new DemoAgentService(
                properties,
                new SiteSessionStore(),
                new TrCacheStore(),
                agentGatewayClient,
                mockMcpGatewayClient,
                new IdGenerator()
        );
        siteSession = demoAgentService.createSiteSession("gwst_demo_001", "z01062668", "demo.user");
    }

    @Test
    void shouldCacheGatewayTokenAndReuseForSameTools() {
        when(agentGatewayClient.requestResourceToken(
                eq("gwst_demo_001"),
                eq("agt_business_001"),
                anyList(),
                eq("http://localhost:18082/agent.html"),
                anyString()
        )).thenReturn(new GatewayTokenResponse("tr_demo_001", 1800L, null, null, null));
        when(mockMcpGatewayClient.extractAuthorizedPermissionPointCodes("tr_demo_001"))
                .thenReturn(java.util.Set.of("erp:contract:r"));
        when(mockMcpGatewayClient.invoke(eq("agt_business_001"), eq("tr_demo_001"), anySet(), anyString()))
                .thenReturn("模拟的合同查询结果");

        ChatResponse first = demoAgentService.handleChat(siteSession.siteSessionId(), "请帮我看一下ERP合同");
        ChatResponse second = demoAgentService.handleChat(siteSession.siteSessionId(), "继续看一下ERP合同");

        assertEquals("answer", first.status());
        assertEquals("gateway", first.source());
        assertNotNull(first.answer());

        assertEquals("answer", second.status());
        assertEquals("cache", second.source());
        assertNotNull(second.answer());

        verify(agentGatewayClient, times(1)).requestResourceToken(
                eq("gwst_demo_001"),
                eq("agt_business_001"),
                anyList(),
                eq("http://localhost:18082/agent.html"),
                anyString()
        );
        verify(mockMcpGatewayClient, times(2)).invoke(eq("agt_business_001"), eq("tr_demo_001"), anySet(), anyString());
    }

    @Test
    void shouldReturnRedirectWhenGatewayRequiresConsent() {
        when(agentGatewayClient.requestResourceToken(
                eq("gwst_demo_001"),
                eq("agt_business_001"),
                anyList(),
                eq("http://localhost:18082/agent.html"),
                anyString()
        )).thenReturn(new GatewayTokenResponse(null, null, "redirect", "http://localhost:18080/gw/auth/authorize?request_id=req_001", "req_001"));

        ChatResponse response = demoAgentService.handleChat(siteSession.siteSessionId(), "请帮我查发票");

        assertEquals("redirect", response.status());
        assertEquals("http://localhost:18080/gw/auth/authorize?request_id=req_001", response.redirectUrl());
        assertNotNull(response.state());
    }
}

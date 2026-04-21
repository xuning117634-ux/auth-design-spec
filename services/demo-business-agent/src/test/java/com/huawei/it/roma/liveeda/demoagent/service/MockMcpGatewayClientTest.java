package com.huawei.it.roma.liveeda.demoagent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.it.roma.liveeda.demoagent.client.PolicyCenterClient;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class MockMcpGatewayClientTest {

    @Mock
    private PolicyCenterClient policyCenterClient;

    @Mock
    private MockMcpClient mockMcpClient;

    private MockMcpGatewayClient mockMcpGatewayClient;

    @BeforeEach
    void setUp() {
        mockMcpGatewayClient = new MockMcpGatewayClient(policyCenterClient, mockMcpClient, new ObjectMapper());
    }

    @Test
    void shouldInvokeToolWhenTrAndStrategyChecksPass() {
        when(policyCenterClient.resolveByTools(Set.of("mcp:contract-server/get_contract")))
                .thenReturn(new PolicyCenterClient.ResolveByToolsResult(
                        List.of("erp:contract:r"),
                        List.of(new PolicyCenterClient.PermissionPointView("erp:contract:r", "ERP 合同的可读权限"))
                ));
        when(policyCenterClient.queryAgentStrategies("agt_business_001", List.of("erp:contract:r")))
                .thenReturn(new PolicyCenterClient.QueryAgentStrategiesResult(
                        "agt_business_001",
                        List.of("erp:contract:r"),
                        List.of()
                ));
        when(policyCenterClient.resolveByCodes(Set.of("erp:contract:r")))
                .thenReturn(new PolicyCenterClient.ResolveByCodesResult(
                        List.of("erp:contract:r"),
                        List.of(new PolicyCenterClient.PermissionPointView("erp:contract:r", "ERP 合同的可读权限")),
                        List.of(new PolicyCenterClient.ToolView("mcp:contract-server/get_contract", "查询合同详情"))
                ));
        when(mockMcpClient.invoke("查一下合同", Set.of("mcp:contract-server/get_contract"), buildTr("z01062668", "erp:contract:r")))
                .thenReturn("ok");

        String result = mockMcpGatewayClient.invoke(
                "agt_business_001",
                buildTr("z01062668", "erp:contract:r"),
                Set.of("mcp:contract-server/get_contract"),
                "查一下合同"
        );

        assertEquals("ok", result);
        verify(mockMcpClient).invoke("查一下合同", Set.of("mcp:contract-server/get_contract"), buildTr("z01062668", "erp:contract:r"));
    }

    @Test
    void shouldRejectWhenRequiredPermissionPointIsNotGrantedInTr() {
        when(policyCenterClient.resolveByTools(Set.of("mcp:invoice-server/query_invoices")))
                .thenReturn(new PolicyCenterClient.ResolveByToolsResult(
                        List.of("erp:invoice:r"),
                        List.of(new PolicyCenterClient.PermissionPointView("erp:invoice:r", "ERP 发票的可读权限"))
                ));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> mockMcpGatewayClient.invoke(
                "agt_business_001",
                buildTr("z01062668", "erp:contract:r"),
                Set.of("mcp:invoice-server/query_invoices"),
                "查发票"
        ));

        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    void shouldRejectWhenDenyStrategyMatches() {
        when(policyCenterClient.resolveByTools(Set.of("mcp:contract-server/get_contract")))
                .thenReturn(new PolicyCenterClient.ResolveByToolsResult(
                        List.of("erp:contract:r"),
                        List.of(new PolicyCenterClient.PermissionPointView("erp:contract:r", "ERP 合同的可读权限"))
                ));
        when(policyCenterClient.queryAgentStrategies("agt_business_001", List.of("erp:contract:r")))
                .thenReturn(new PolicyCenterClient.QueryAgentStrategiesResult(
                        "agt_business_001",
                        List.of("erp:contract:r"),
                        List.of(new PolicyCenterClient.AgentStrategyView(
                                "stg_001",
                                "agt_business_001",
                                "erp:contract:r",
                                new PolicyCenterClient.StrategyConditionView("subject.user_id", "equals", List.of("z01062668")),
                                "DENY",
                                "ACTIVE"
                        ))
                ));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> mockMcpGatewayClient.invoke(
                "agt_business_001",
                buildTr("z01062668", "erp:contract:r"),
                Set.of("mcp:contract-server/get_contract"),
                "查合同"
        ));

        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    void shouldRejectWhenPermitExistsButDoesNotMatch() {
        when(policyCenterClient.resolveByTools(Set.of("mcp:contract-server/get_contract")))
                .thenReturn(new PolicyCenterClient.ResolveByToolsResult(
                        List.of("erp:contract:r"),
                        List.of(new PolicyCenterClient.PermissionPointView("erp:contract:r", "ERP 合同的可读权限"))
                ));
        when(policyCenterClient.queryAgentStrategies("agt_business_001", List.of("erp:contract:r")))
                .thenReturn(new PolicyCenterClient.QueryAgentStrategiesResult(
                        "agt_business_001",
                        List.of("erp:contract:r"),
                        List.of(new PolicyCenterClient.AgentStrategyView(
                                "stg_001",
                                "agt_business_001",
                                "erp:contract:r",
                                new PolicyCenterClient.StrategyConditionView("subject.user_id", "in", List.of("other_user")),
                                "PERMIT",
                                "ACTIVE"
                        ))
                ));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> mockMcpGatewayClient.invoke(
                "agt_business_001",
                buildTr("z01062668", "erp:contract:r"),
                Set.of("mcp:contract-server/get_contract"),
                "查合同"
        ));

        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    void shouldRejectWhenToolIsNotCoveredByTrResolvedTools() {
        when(policyCenterClient.resolveByTools(Set.of("mcp:contract-server/get_contract")))
                .thenReturn(new PolicyCenterClient.ResolveByToolsResult(
                        List.of("erp:contract:r"),
                        List.of(new PolicyCenterClient.PermissionPointView("erp:contract:r", "ERP 合同的可读权限"))
                ));
        when(policyCenterClient.queryAgentStrategies("agt_business_001", List.of("erp:contract:r")))
                .thenReturn(new PolicyCenterClient.QueryAgentStrategiesResult(
                        "agt_business_001",
                        List.of("erp:contract:r"),
                        List.of()
                ));
        when(policyCenterClient.resolveByCodes(Set.of("erp:contract:r")))
                .thenReturn(new PolicyCenterClient.ResolveByCodesResult(
                        List.of("erp:contract:r"),
                        List.of(new PolicyCenterClient.PermissionPointView("erp:contract:r", "ERP 合同的可读权限")),
                        List.of(new PolicyCenterClient.ToolView("mcp:other-server/another_tool", "其他工具"))
                ));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> mockMcpGatewayClient.invoke(
                "agt_business_001",
                buildTr("z01062668", "erp:contract:r"),
                Set.of("mcp:contract-server/get_contract"),
                "查合同"
        ));

        assertEquals(403, exception.getStatusCode().value());
        verify(policyCenterClient).resolveByCodes(anySet());
    }

    private String buildTr(String userId, String... permissionPointCodes) {
        String header = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(("""
                        {
                          "agency_user": {
                            "user_id": "%s",
                            "authorizedPermissionPoints": [%s]
                          }
                        }
                        """.formatted(
                        userId,
                        String.join(",", List.of(permissionPointCodes).stream()
                                .map(code -> "{\"code\":\"" + code + "\",\"displayNameZh\":\"demo\"}")
                                .toList())
                )).getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".";
    }
}

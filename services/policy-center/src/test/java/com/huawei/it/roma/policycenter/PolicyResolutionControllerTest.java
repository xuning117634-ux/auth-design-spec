package com.huawei.it.roma.policycenter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PolicyResolutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldResolveToolsToPermissionPoints() throws Exception {
        mockMvc.perform(post("/internal/v1/permission-points/resolve-by-tools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agentId": "agt_business_001",
                                  "requiredTools": [
                                    "mcp:contract-server/get_contract"
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredPermissionPointCodes[0]").value("erp:contract:r"))
                .andExpect(jsonPath("$.permissionPoints[0].code").value("erp:contract:r"))
                .andExpect(jsonPath("$.permissionPoints[0].displayNameZh").value("ERP 合同的可读权限"));
    }

    @Test
    void shouldResolveCodesToTools() throws Exception {
        mockMvc.perform(post("/internal/v1/permission-points/resolve-by-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "permissionPointCodes": [
                                    "erp:contract:r"
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowedTools[0]").value("mcp:contract-server/get_contract"))
                .andExpect(jsonPath("$.permissionPoints[0].code").value("erp:contract:r"))
                .andExpect(jsonPath("$.toolItems[0].toolId").value("mcp:contract-server/get_contract"));
    }

    @Test
    void shouldQueryAgentStrategies() throws Exception {
        mockMvc.perform(post("/internal/v1/agent-strategies/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agentId": "agt_business_001",
                                  "permissionPointCodes": [
                                    "erp:contract:r",
                                    "erp:invoice:r"
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategies.length()").value(2))
                .andExpect(jsonPath("$.strategies[0].permissionPointCode").value("erp:contract:r"))
                .andExpect(jsonPath("$.strategies[1].permissionPointCode").value("erp:invoice:r"));
    }

    @Test
    void shouldRejectUnknownTool() throws Exception {
        mockMvc.perform(post("/internal/v1/permission-points/resolve-by-tools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agentId": "agt_business_001",
                                  "requiredTools": [
                                    "mcp:unknown/tool"
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}

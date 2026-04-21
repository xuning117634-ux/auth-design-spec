package com.huawei.it.roma.policycenter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PolicyResolutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldResolveToolsToPermissionPointsWithoutAgentId() throws Exception {
        mockMvc.perform(post("/internal/v1/permission-points/resolve-by-tools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requiredTools": [
                                    "mcp:contract-server/get_contract"
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredPermissionPointCodes[0]").value("erp:contract:r"))
                .andExpect(jsonPath("$.permissionPoints[0].code").value("erp:contract:r"));
    }

    @Test
    void shouldResolveCodesToPermissionPointsAndTools() throws Exception {
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
                .andExpect(jsonPath("$.permissionPoints[0].code").value("erp:contract:r"))
                .andExpect(jsonPath("$.tools[0].toolId").value("mcp:contract-server/get_contract"));
    }

    @Test
    void shouldQueryOnlyActiveStrategies() throws Exception {
        mockMvc.perform(put("/internal/v1/agent-strategies/batch-upsert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agentId": "agt_business_001",
                                  "items": [
                                    {
                                      "strategyId": "stg_contract_permit_demo_user",
                                      "permissionPointCode": "erp:contract:r",
                                      "conditions": {
                                        "field": "subject.user_id",
                                        "operator": "in",
                                        "values": ["z01062668"]
                                      },
                                      "effect": "PERMIT",
                                      "status": "INACTIVE"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upsertedCount").value(1));

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
                .andExpect(jsonPath("$.strategies.length()").value(1))
                .andExpect(jsonPath("$.strategies[0].permissionPointCode").value("erp:invoice:r"));
    }

    @Test
    void shouldDisablePermissionPointFromRuntimeIndexes() throws Exception {
        mockMvc.perform(put("/internal/v1/permission-points/batch-upsert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "source": "mcp_gateway",
                                  "items": [
                                    {
                                      "permissionPointCode": "erp:contract:r",
                                      "displayNameZh": "ERP 合同的可读权限",
                                      "description": "允许读取 ERP 合同数据",
                                      "boundTools": [
                                        "mcp:contract-server/get_contract"
                                      ],
                                      "status": "INACTIVE"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upsertedCount").value(1));

        mockMvc.perform(post("/internal/v1/permission-points/resolve-by-tools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requiredTools": [
                                    "mcp:contract-server/get_contract"
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        mockMvc.perform(post("/internal/v1/permission-points/resolve-by-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "permissionPointCodes": [
                                    "erp:contract:r"
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void shouldRestorePermissionPointByUpsertToActive() throws Exception {
        mockMvc.perform(put("/internal/v1/permission-points/batch-upsert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "source": "mcp_gateway",
                                  "items": [
                                    {
                                      "permissionPointCode": "erp:contract:r",
                                      "displayNameZh": "ERP 合同的可读权限",
                                      "description": "允许读取 ERP 合同数据",
                                      "boundTools": [
                                        "mcp:contract-server/get_contract"
                                      ],
                                      "status": "INACTIVE"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(put("/internal/v1/permission-points/batch-upsert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "source": "mcp_gateway",
                                  "items": [
                                    {
                                      "permissionPointCode": "erp:contract:r",
                                      "displayNameZh": "ERP 合同的可读权限",
                                      "description": "允许读取 ERP 合同数据",
                                      "boundTools": [
                                        "mcp:contract-server/get_contract"
                                      ],
                                      "status": "ACTIVE"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upsertedCount").value(1));

        mockMvc.perform(post("/internal/v1/permission-points/resolve-by-tools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requiredTools": [
                                    "mcp:contract-server/get_contract"
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredPermissionPointCodes[0]").value("erp:contract:r"));
    }

    @Test
    void shouldRejectUnknownTool() throws Exception {
        mockMvc.perform(post("/internal/v1/permission-points/resolve-by-tools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requiredTools": [
                                    "mcp:unknown/tool"
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}

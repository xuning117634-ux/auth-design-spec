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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PolicyResolutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    void shouldAcceptStringStyleBoundToolsInBatchUpsert() throws Exception {
        mockMvc.perform(put("/internal/v1/permission-points/batch-upsert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "source": "mcp_gateway",
                                  "items": [
                                    {
                                      "permissionPointCode": "erp:contract:r",
                                      "enterprise": "ent_001",
                                      "appId": "erp",
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
                .andExpect(jsonPath("$.tools[0].toolId").value("mcp:contract-server/get_contract"));
    }

    @Test
    void shouldRejectCommaInPermissionPointCodeWhenUpsertingPermissionPoint() throws Exception {
        mockMvc.perform(put("/internal/v1/permission-points/batch-upsert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "source": "mcp_gateway",
                                  "items": [
                                    {
                                      "permissionPointCode": "erp:contract:r,bad",
                                      "enterprise": "ent_001",
                                      "appId": "erp",
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
                .andExpect(status().isBadRequest());
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
    void shouldDisablePermissionPointFromRuntimeQueries() throws Exception {
        mockMvc.perform(put("/internal/v1/permission-points/batch-upsert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "source": "mcp_gateway",
                                  "items": [
                                    {
                                      "permissionPointCode": "erp:contract:r",
                                      "enterprise": "ent_001",
                                      "appId": "erp",
                                      "displayNameZh": "ERP 合同的可读权限",
                                      "description": "允许读取 ERP 合同数据",
                                      "boundTools": [
                                        {
                                          "toolId": "mcp:contract-server/get_contract",
                                          "displayNameZh": "查询合同详情"
                                        }
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredPermissionPointCodes.length()").value(0))
                .andExpect(jsonPath("$.permissionPoints.length()").value(0));

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
                .andExpect(jsonPath("$.permissionPointCodes.length()").value(0))
                .andExpect(jsonPath("$.permissionPoints.length()").value(0))
                .andExpect(jsonPath("$.tools.length()").value(0));
    }

    @Test
    void shouldRestorePermissionPointByUpsertToActive() throws Exception {
        upsertContractPermissionPoint("INACTIVE");

        mockMvc.perform(put("/internal/v1/permission-points/batch-upsert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contractPermissionPointPayload("ACTIVE")))
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
    void shouldQueryPermissionPointsByEnterpriseAndOptionalFilters() throws Exception {
        mockMvc.perform(post("/internal/v1/permission-points/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enterprise": "ent_001",
                                  "appId": "erp",
                                  "status": "ACTIVE",
                                  "permissionPointCodes": [
                                    "erp:contract:r"
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissionPoints.length()").value(1))
                .andExpect(jsonPath("$.permissionPoints[0].permissionPointCode").value("erp:contract:r"))
                .andExpect(jsonPath("$.permissionPoints[0].enterprise").value("ent_001"))
                .andExpect(jsonPath("$.permissionPoints[0].appId").value("erp"))
                .andExpect(jsonPath("$.permissionPoints[0].boundTools[0].toolId")
                        .value("mcp:contract-server/get_contract"));

        mockMvc.perform(post("/internal/v1/permission-points/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enterprise": "ent_001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissionPoints.length()").value(3));

        mockMvc.perform(post("/internal/v1/permission-points/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "appId": "erp"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpsertAgentPermissionPointSnapshotWithSortedUniqueCodes() throws Exception {
        mockMvc.perform(put("/internal/v1/agent-permission-points/batch-upsert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agentId": "agt_business_001",
                                  "enterprise": "ent_001",
                                  "source": "agent-gateway",
                                  "permissionPointCodes": [
                                    "erp:invoice:r",
                                    "erp:contract:r",
                                    "erp:contract:r"
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentId").value("agt_business_001"))
                .andExpect(jsonPath("$.enterprise").value("ent_001"))
                .andExpect(jsonPath("$.permissionPointCount").value(2))
                .andExpect(jsonPath("$.permissionPointCodes[0]").value("erp:contract:r"))
                .andExpect(jsonPath("$.permissionPointCodes[1]").value("erp:invoice:r"));
    }

    @Test
    void shouldIgnoreBlankCodesWhenUpsertingAgentPermissionPointSnapshot() throws Exception {
        mockMvc.perform(put("/internal/v1/agent-permission-points/batch-upsert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agentId": "agt_business_001",
                                  "enterprise": "ent_001",
                                  "source": "agent-gateway",
                                  "permissionPointCodes": [
                                    "",
                                    "  ",
                                    "erp:contract:r"
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissionPointCount").value(1))
                .andExpect(jsonPath("$.permissionPointCodes[0]").value("erp:contract:r"));
    }

    @Test
    void shouldOverwriteAgentPermissionPointSnapshotWhenSubscriptionChanges() throws Exception {
        upsertAgentPermissionPointSnapshot("""
                [
                  "erp:contract:r",
                  "erp:invoice:r",
                  "erp:report:r"
                ]
                """);

        mockMvc.perform(put("/internal/v1/agent-permission-points/batch-upsert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agentId": "agt_business_001",
                                  "enterprise": "ent_001",
                                  "source": "agent-gateway",
                                  "permissionPointCodes": [
                                    "erp:contract:r",
                                    "erp:report:r"
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissionPointCount").value(2))
                .andExpect(jsonPath("$.permissionPointCodes[0]").value("erp:contract:r"))
                .andExpect(jsonPath("$.permissionPointCodes[1]").value("erp:report:r"));
    }

    @Test
    void shouldClearAgentPermissionPointSnapshot() throws Exception {
        mockMvc.perform(put("/internal/v1/agent-permission-points/batch-upsert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agentId": "agt_business_001",
                                  "enterprise": "ent_001",
                                  "source": "agent-gateway",
                                  "permissionPointCodes": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissionPointCount").value(0))
                .andExpect(jsonPath("$.permissionPointCodes.length()").value(0));
    }

    @Test
    void shouldRejectAgentPermissionPointSnapshotForUnknownPermissionPoint() throws Exception {
        mockMvc.perform(put("/internal/v1/agent-permission-points/batch-upsert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agentId": "agt_business_001",
                                  "enterprise": "ent_001",
                                  "source": "agent-gateway",
                                  "permissionPointCodes": [
                                    "erp:unknown:r"
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectCommaInAgentPermissionPointSnapshot() throws Exception {
        mockMvc.perform(put("/internal/v1/agent-permission-points/batch-upsert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agentId": "agt_business_001",
                                  "enterprise": "ent_001",
                                  "source": "agent-gateway",
                                  "permissionPointCodes": [
                                    "erp:contract:r,bad"
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest());
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredPermissionPointCodes.length()").value(0))
                .andExpect(jsonPath("$.permissionPoints.length()").value(0));
    }

    @Test
    void shouldReturnEmptyWhenQueryStrategiesForUnknownPermissionPoint() throws Exception {
        mockMvc.perform(post("/internal/v1/agent-strategies/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agentId": "agt_business_001",
                                  "permissionPointCodes": [
                                    "erp:unknown:r"
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissionPointCodes.length()").value(0))
                .andExpect(jsonPath("$.strategies.length()").value(0));
    }

    @Test
    void shouldReturnEmptyWhenResolveByCodesForUnknownPermissionPoint() throws Exception {
        mockMvc.perform(post("/internal/v1/permission-points/resolve-by-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "permissionPointCodes": [
                                    "erp:unknown:r"
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissionPointCodes.length()").value(0))
                .andExpect(jsonPath("$.permissionPoints.length()").value(0))
                .andExpect(jsonPath("$.tools.length()").value(0));
    }

    @Test
    void shouldHardDeletePermissionPointAndCascadeRelatedData() throws Exception {
        mockMvc.perform(post("/internal/v1/permission-points/batch-hard-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enterprise": "ent_001",
                                  "permissionPointCodes": [
                                    "erp:contract:r"
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedCount").value(1))
                .andExpect(jsonPath("$.items[0].id").value("erp:contract:r"))
                .andExpect(jsonPath("$.items[0].result").value("DELETED"));

        mockMvc.perform(post("/internal/v1/permission-points/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enterprise": "ent_001",
                                  "permissionPointCodes": [
                                    "erp:contract:r"
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissionPoints.length()").value(0));

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
                .andExpect(jsonPath("$.permissionPointCodes.length()").value(0))
                .andExpect(jsonPath("$.permissionPoints.length()").value(0))
                .andExpect(jsonPath("$.tools.length()").value(0));

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
                .andExpect(jsonPath("$.permissionPointCodes.length()").value(1))
                .andExpect(jsonPath("$.permissionPointCodes[0]").value("erp:invoice:r"))
                .andExpect(jsonPath("$.strategies.length()").value(1))
                .andExpect(jsonPath("$.strategies[0].permissionPointCode").value("erp:invoice:r"));

        String snapshot = jdbcTemplate.queryForObject("""
                SELECT permission_point_codes
                FROM pc_agent_permission_point
                WHERE agent_id = 'agt_business_001'
                  AND enterprise = 'ent_001'
                """, String.class);
        org.assertj.core.api.Assertions.assertThat(snapshot)
                .isEqualTo("erp:invoice:r,erp:report:r");

        mockMvc.perform(post("/internal/v1/permission-points/batch-hard-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enterprise": "ent_001",
                                  "permissionPointCodes": [
                                    "erp:contract:r"
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedCount").value(0))
                .andExpect(jsonPath("$.items[0].id").value("erp:contract:r"))
                .andExpect(jsonPath("$.items[0].result").value("NOT_FOUND"));
    }

    @Test
    void shouldKeepToolWhenHardDeletingPermissionPoint() throws Exception {
        mockMvc.perform(put("/internal/v1/permission-points/batch-upsert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "source": "mcp_gateway",
                                  "items": [
                                    {
                                      "permissionPointCode": "erp:contract:detail:r",
                                      "enterprise": "ent_001",
                                      "appId": "erp",
                                      "displayNameZh": "Contract detail read permission",
                                      "description": "Allow reading contract detail data",
                                      "boundTools": [
                                        "mcp:contract-server/get_contract"
                                      ],
                                      "status": "ACTIVE"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/internal/v1/permission-points/batch-hard-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enterprise": "ent_001",
                                  "permissionPointCodes": [
                                    "erp:contract:r"
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedCount").value(1));

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
                .andExpect(jsonPath("$.requiredPermissionPointCodes.length()").value(1))
                .andExpect(jsonPath("$.requiredPermissionPointCodes[0]").value("erp:contract:detail:r"));
    }

    @Test
    void shouldHardDeleteAgentStrategyAndConditionValues() throws Exception {
        mockMvc.perform(post("/internal/v1/agent-strategies/batch-hard-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agentId": "agt_business_001",
                                  "strategyIds": [
                                    "stg_invoice_deny_blocked_user"
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedCount").value(1))
                .andExpect(jsonPath("$.items[0].id").value("stg_invoice_deny_blocked_user"))
                .andExpect(jsonPath("$.items[0].result").value("DELETED"));

        Integer conditionValueCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM pc_agent_strategy_condition_value
                WHERE strategy_id = 'stg_invoice_deny_blocked_user'
                """, Integer.class);
        org.assertj.core.api.Assertions.assertThat(conditionValueCount).isZero();

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
                .andExpect(jsonPath("$.strategies[0].strategyId").value("stg_contract_permit_demo_user"));

        mockMvc.perform(post("/internal/v1/agent-strategies/batch-hard-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agentId": "agt_business_001",
                                  "strategyIds": [
                                    "stg_invoice_deny_blocked_user"
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedCount").value(0))
                .andExpect(jsonPath("$.items[0].id").value("stg_invoice_deny_blocked_user"))
                .andExpect(jsonPath("$.items[0].result").value("NOT_FOUND"));
    }

    @Test
    void shouldRejectInvalidHardDeleteRequests() throws Exception {
        mockMvc.perform(post("/internal/v1/permission-points/batch-hard-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "permissionPointCodes": [
                                    "erp:contract:r"
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/internal/v1/permission-points/batch-hard-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enterprise": "ent_001",
                                  "permissionPointCodes": []
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/internal/v1/agent-strategies/batch-hard-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "strategyIds": [
                                    "stg_invoice_deny_blocked_user"
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/internal/v1/agent-strategies/batch-hard-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agentId": "agt_business_001",
                                  "strategyIds": []
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    private void upsertContractPermissionPoint(String status) throws Exception {
        mockMvc.perform(put("/internal/v1/permission-points/batch-upsert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contractPermissionPointPayload(status)))
                .andExpect(status().isOk());
    }

    private void upsertAgentPermissionPointSnapshot(String permissionPointCodesJson) throws Exception {
        mockMvc.perform(put("/internal/v1/agent-permission-points/batch-upsert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agentId": "agt_business_001",
                                  "enterprise": "ent_001",
                                  "source": "agent-gateway",
                                  "permissionPointCodes": %s
                                }
                                """.formatted(permissionPointCodesJson)))
                .andExpect(status().isOk());
    }

    private String contractPermissionPointPayload(String status) {
        return """
                {
                  "source": "mcp_gateway",
                  "items": [
                    {
                      "permissionPointCode": "erp:contract:r",
                      "enterprise": "ent_001",
                      "appId": "erp",
                      "displayNameZh": "ERP 合同的可读权限",
                      "description": "允许读取 ERP 合同数据",
                      "boundTools": [
                        {
                          "toolId": "mcp:contract-server/get_contract",
                          "displayNameZh": "查询合同详情"
                        }
                      ],
                      "status": "%s"
                    }
                  ]
                }
                """.formatted(status);
    }
}

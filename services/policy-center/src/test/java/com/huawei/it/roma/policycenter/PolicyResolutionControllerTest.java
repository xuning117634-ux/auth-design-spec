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
    void shouldResolveToolsToPolicies() throws Exception {
        mockMvc.perform(post("/internal/v1/policies/resolve-by-tools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agentId": "agt_business_001",
                                  "requiredTools": [
                                    "mcp:financial-report-server/list_report_categories",
                                    "mcp:financial-report-server/query_monthly_report"
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredPolicyCodes[0]").value("erp:report:category:list"))
                .andExpect(jsonPath("$.requiredPolicyCodes[1]").value("erp:report:read"))
                .andExpect(jsonPath("$.policyItems.length()").value(2));
    }

    @Test
    void shouldResolveCodesToTools() throws Exception {
        mockMvc.perform(post("/internal/v1/policies/resolve-by-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "policyCodes": [
                                    "erp:report:read",
                                    "erp:report:category:list"
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowedTools[0]").value("mcp:financial-report-server/list_report_categories"))
                .andExpect(jsonPath("$.allowedTools[1]").value("mcp:financial-report-server/query_monthly_report"))
                .andExpect(jsonPath("$.toolItems.length()").value(2));
    }

    @Test
    void shouldRejectUnknownTool() throws Exception {
        mockMvc.perform(post("/internal/v1/policies/resolve-by-tools")
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

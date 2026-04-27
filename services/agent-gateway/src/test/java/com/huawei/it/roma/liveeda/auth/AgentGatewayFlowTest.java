package com.huawei.it.roma.liveeda.auth;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.it.roma.liveeda.auth.client.PolicyCenterClient;
import com.huawei.it.roma.liveeda.auth.client.PolicyResolutionResult;
import com.huawei.it.roma.liveeda.auth.domain.AuthorizedPermissionPoint;
import com.huawei.it.roma.liveeda.auth.web.ResourceTokenRequest;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("mock")
class AgentGatewayFlowTest {

    private static final AuthorizedPermissionPoint CONTRACT_READ_PERMISSION =
            new AuthorizedPermissionPoint("erp:contract:r", "ERP 合同的可读权限");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PolicyCenterClient policyCenterClient;

    @Test
    void shouldCompleteBaseLoginAndConsentWithOneTimeTickets() throws Exception {
        when(policyCenterClient.resolveByTools(anySet()))
                .thenReturn(new PolicyResolutionResult(Set.of("erp:contract:r"), List.of(CONTRACT_READ_PERMISSION)));
        when(policyCenterClient.resolveByCodes(anySet()))
                .thenReturn(List.of(CONTRACT_READ_PERMISSION));

        MvcResult startLoginResult = mockMvc.perform(get("/gw/auth/login")
                        .param("agent_id", "agt_business_001")
                        .param("return_url", "http://localhost:18082/agent")
                        .param("state", "outer_login_state"))
                .andExpect(status().isFound())
                .andReturn();

        URI mockIdaasBaseUri = URI.create(startLoginResult.getResponse().getRedirectedUrl());
        Map<String, String> baseParams = queryParams(mockIdaasBaseUri);
        org.junit.jupiter.api.Assertions.assertEquals("agent_gateway_client", baseParams.get("client_id"));
        org.junit.jupiter.api.Assertions.assertEquals("agt_business_001", baseParams.get("agent_id"));

        MvcResult approveBaseResult = mockMvc.perform(post("/mock/idaas/approve")
                        .param("flow", baseParams.get("flow"))
                        .param("redirect_uri", baseParams.get("redirect_uri"))
                        .param("scope", baseParams.get("scope"))
                        .param("state", baseParams.get("state"))
                        .param("user_id", "z01062668")
                        .param("username", "demo.user")
                        .param("password", "MockPassword@123"))
                .andExpect(status().isFound())
                .andReturn();

        URI baseCallbackUri = URI.create(approveBaseResult.getResponse().getRedirectedUrl());
        MvcResult baseCallbackResult = mockMvc.perform(get(baseCallbackUri.getPath())
                        .queryParam("code", queryParams(baseCallbackUri).get("code"))
                        .queryParam("state", queryParams(baseCallbackUri).get("state")))
                .andExpect(status().isFound())
                .andReturn();

        MockHttpServletResponse baseResponse = baseCallbackResult.getResponse();
        assertNull(baseResponse.getCookie("gw_session_id"));
        String businessRedirect = baseResponse.getRedirectedUrl();
        Map<String, String> businessRedirectParams = queryParams(URI.create(businessRedirect));
        String ticketST = businessRedirectParams.get("ticketST");
        assertNull(businessRedirectParams.get("gw_session_token"));
        assertNull(businessRedirectParams.get("user_id"));

        mockMvc.perform(post("/gw/auth/ticket/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agent_id": "agt_business_001",
                                  "ticketST": "%s"
                                }
                                """.formatted(ticketST)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.userId").value("z01062668"))
                .andExpect(jsonPath("$.user.username").value("demo.user"));

        mockMvc.perform(post("/gw/auth/ticket/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agent_id": "agt_business_001",
                                  "ticketST": "%s"
                                }
                                """.formatted(ticketST)))
                .andExpect(status().isUnauthorized());

        ResourceTokenRequest resourceTokenRequest = new ResourceTokenRequest(
                "agt_business_001",
                List.of("mcp:contract-server/get_contract"),
                "http://localhost:18082/agent.html",
                "outer_chat_state",
                null
        );

        MvcResult firstTokenResult = mockMvc.perform(post("/gw/token/resource-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resourceTokenRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REDIRECT_REQUIRED"))
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.redirectUrl").isNotEmpty())
                .andReturn();

        String authorizeRedirectUrl = objectMapper.readTree(firstTokenResult.getResponse().getContentAsString())
                .get("redirectUrl")
                .asText();
        URI authorizeUri = URI.create(authorizeRedirectUrl);
        String requestId = queryParams(authorizeUri).get("request_id");

        MvcResult startConsentResult = mockMvc.perform(get(authorizeUri.getPath())
                        .queryParam("request_id", requestId))
                .andExpect(status().isFound())
                .andReturn();

        URI mockIdaasConsentUri = URI.create(startConsentResult.getResponse().getRedirectedUrl());
        Map<String, String> consentParams = queryParams(mockIdaasConsentUri);
        org.junit.jupiter.api.Assertions.assertEquals("agent_gateway_client", consentParams.get("client_id"));
        org.junit.jupiter.api.Assertions.assertEquals("agt_business_001", consentParams.get("agent_id"));

        MvcResult approveConsentResult = mockMvc.perform(post("/mock/idaas/approve")
                        .param("flow", consentParams.get("flow"))
                        .param("redirect_uri", consentParams.get("redirect_uri"))
                        .param("scope", consentParams.get("scope"))
                        .param("state", consentParams.get("state"))
                        .param("approved", "true"))
                .andExpect(status().isFound())
                .andReturn();

        URI consentCallbackUri = URI.create(approveConsentResult.getResponse().getRedirectedUrl());
        MvcResult consentCallbackResult = mockMvc.perform(get(consentCallbackUri.getPath())
                        .queryParam("code", queryParams(consentCallbackUri).get("code"))
                        .queryParam("state", queryParams(consentCallbackUri).get("state")))
                .andExpect(status().isFound())
                .andReturn();

        URI businessConsentRedirect = URI.create(consentCallbackResult.getResponse().getRedirectedUrl());
        Map<String, String> consentRedirectParams = queryParams(businessConsentRedirect);
        String tokenResultTicket = consentRedirectParams.get("token_result_ticket");

        mockMvc.perform(post("/gw/token/result/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agent_id": "agt_business_001",
                                  "request_id": "%s",
                                  "token_result_ticket": "%s"
                                }
                                """.formatted(requestId, tokenResultTicket)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TOKEN_READY"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andExpect(jsonPath("$.consentedScopes[0].code").value("erp:contract:r"));

        mockMvc.perform(post("/gw/token/result/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agent_id": "agt_business_001",
                                  "request_id": "%s",
                                  "token_result_ticket": "%s"
                                }
                                """.formatted(requestId, tokenResultTicket)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectResourceTokenWhenPermissionPointIsNotSubscribed() throws Exception {
        when(policyCenterClient.resolveByTools(anySet()))
                .thenReturn(new PolicyResolutionResult(
                        Set.of("erp:invoice:r"),
                        List.of(new AuthorizedPermissionPoint("erp:invoice:r", "ERP 发票的可读权限"))
                ));

        ResourceTokenRequest resourceTokenRequest = new ResourceTokenRequest(
                "agt_contract_only_001",
                List.of("mcp:invoice-server/query_invoices"),
                "http://localhost:18082/agent.html",
                "outer_chat_state",
                null
        );

        mockMvc.perform(post("/gw/token/resource-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resourceTokenRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Agent has not subscribed all required permission points"));
    }

    private Map<String, String> queryParams(URI uri) {
        return UriComponentsBuilder.fromUri(uri)
                .build()
                .getQueryParams()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getFirst()));
    }
}

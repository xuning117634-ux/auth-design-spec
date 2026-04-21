package com.huawei.it.roma.liveeda.auth;

import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.it.roma.liveeda.auth.client.PolicyCenterClient;
import com.huawei.it.roma.liveeda.auth.client.PolicyResolutionResult;
import com.huawei.it.roma.liveeda.auth.domain.AuthorizedPermissionPoint;
import com.huawei.it.roma.liveeda.auth.web.ResourceTokenRequest;
import jakarta.servlet.http.Cookie;
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
    void shouldCompleteBaseLoginAndConsentThenReuseTr() throws Exception {
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
                .andExpect(cookie().exists("gw_session_id"))
                .andReturn();

        MockHttpServletResponse baseResponse = baseCallbackResult.getResponse();
        String businessRedirect = baseResponse.getRedirectedUrl();
        Map<String, String> businessRedirectParams = queryParams(URI.create(businessRedirect));
        String gwSessionToken = businessRedirectParams.get("gw_session_token");
        Cookie gatewaySessionCookie = baseResponse.getCookie("gw_session_id");

        ResourceTokenRequest resourceTokenRequest = new ResourceTokenRequest(
                "agt_business_001",
                List.of("mcp:contract-server/get_contract"),
                "http://localhost:18082/agent.html",
                "outer_chat_state"
        );

        MvcResult firstTokenResult = mockMvc.perform(post("/gw/token/resource-token")
                        .header("Authorization", "Bearer " + gwSessionToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resourceTokenRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("redirect"))
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.redirectUrl").isNotEmpty())
                .andReturn();

        String authorizeRedirectUrl = objectMapper.readTree(firstTokenResult.getResponse().getContentAsString())
                .get("redirectUrl")
                .asText();
        URI authorizeUri = URI.create(authorizeRedirectUrl);
        String requestId = queryParams(authorizeUri).get("request_id");

        MvcResult startConsentResult = mockMvc.perform(get(authorizeUri.getPath())
                        .queryParam("request_id", requestId)
                        .cookie(gatewaySessionCookie))
                .andExpect(status().isFound())
                .andReturn();

        URI mockIdaasConsentUri = URI.create(startConsentResult.getResponse().getRedirectedUrl());
        Map<String, String> consentParams = queryParams(mockIdaasConsentUri);

        MvcResult approveConsentResult = mockMvc.perform(post("/mock/idaas/approve")
                        .param("flow", consentParams.get("flow"))
                        .param("redirect_uri", consentParams.get("redirect_uri"))
                        .param("scope", consentParams.get("scope"))
                        .param("state", consentParams.get("state"))
                        .param("approved", "true"))
                .andExpect(status().isFound())
                .andReturn();

        URI consentCallbackUri = URI.create(approveConsentResult.getResponse().getRedirectedUrl());
        mockMvc.perform(get(consentCallbackUri.getPath())
                        .queryParam("code", queryParams(consentCallbackUri).get("code"))
                        .queryParam("state", queryParams(consentCallbackUri).get("state")))
                .andExpect(status().isFound());

        mockMvc.perform(post("/gw/token/resource-token")
                        .header("Authorization", "Bearer " + gwSessionToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resourceTokenRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.redirectUrl").doesNotExist())
                .andExpect(jsonPath("$.requestId").doesNotExist());
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

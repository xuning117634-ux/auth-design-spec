package com.huawei.it.roma.liveeda.demoagent.client;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.huawei.it.roma.liveeda.demoagent.config.DemoAgentProperties;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class AgentGatewayClient {

    private final RestClient.Builder restClientBuilder;
    private final DemoAgentProperties properties;

    public LoginTicketExchangeResponse exchangeLoginTicket(String agentId, String ticketST) {
        RestClient restClient = restClientBuilder.baseUrl(properties.getGatewayBaseUrl()).build();
        return restClient.post()
                .uri("/gw/auth/ticket/exchange")
                .body(new LoginTicketExchangeRequest(agentId, ticketST))
                .retrieve()
                .body(LoginTicketExchangeResponse.class);
    }

    public GatewayTokenResponse requestResourceToken(
            String agentId,
            List<String> requiredTools,
            String returnUrl,
            String state
    ) {
        RestClient restClient = restClientBuilder.baseUrl(properties.getGatewayBaseUrl()).build();
        return restClient.post()
                .uri("/gw/token/resource-token")
                .body(new GatewayTokenRequest(agentId, requiredTools, returnUrl, state))
                .retrieve()
                .body(GatewayTokenResponse.class);
    }

    public GatewayTokenResponse exchangeTokenResult(String agentId, String requestId, String tokenResultTicket) {
        RestClient restClient = restClientBuilder.baseUrl(properties.getGatewayBaseUrl()).build();
        return restClient.post()
                .uri("/gw/token/result/exchange")
                .body(new TokenResultExchangeRequest(agentId, requestId, tokenResultTicket))
                .retrieve()
                .body(GatewayTokenResponse.class);
    }

    private record LoginTicketExchangeRequest(
            @JsonAlias("agent_id") String agentId,
            String ticketST
    ) {
    }

    private record GatewayTokenRequest(
            @JsonAlias("agent_id") String agentId,
            @JsonAlias("required_tools") List<String> requiredTools,
            @JsonAlias("return_url") String returnUrl,
            String state
    ) {
    }

    private record TokenResultExchangeRequest(
            @JsonAlias("agent_id") String agentId,
            @JsonAlias("request_id") String requestId,
            @JsonAlias("token_result_ticket") String tokenResultTicket
    ) {
    }
}

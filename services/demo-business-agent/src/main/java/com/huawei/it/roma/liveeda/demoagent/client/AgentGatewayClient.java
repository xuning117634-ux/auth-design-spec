package com.huawei.it.roma.liveeda.demoagent.client;

import com.huawei.it.roma.liveeda.demoagent.config.DemoAgentProperties;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class AgentGatewayClient {

    private final RestClient.Builder restClientBuilder;
    private final DemoAgentProperties properties;

    public GatewayTokenResponse requestResourceToken(
            String gwSessionToken,
            String agentId,
            List<String> requiredTools,
            String returnUrl,
            String state
    ) {
        RestClient restClient = restClientBuilder.baseUrl(properties.getGatewayBaseUrl()).build();
        return restClient.post()
                .uri("/gw/token/resource-token")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + gwSessionToken)
                .body(new GatewayTokenRequest(agentId, requiredTools, returnUrl, state))
                .retrieve()
                .body(GatewayTokenResponse.class);
    }

    private record GatewayTokenRequest(
            String agentId,
            List<String> requiredTools,
            String returnUrl,
            String state
    ) {
    }
}

package com.huawei.it.roma.liveeda.auth.client.iam;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.huawei.it.roma.liveeda.auth.config.IamProperties;
import com.huawei.it.roma.liveeda.auth.domain.AgentRegistryEntry;
import com.huawei.it.roma.liveeda.auth.domain.IssuedToken;
import com.huawei.it.roma.liveeda.auth.web.GatewayException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Profile("real")
@RequiredArgsConstructor
public class RealIamAssumeAgentTokenClient implements IamAssumeAgentTokenClient {

    private final RestClient.Builder restClientBuilder;
    private final IamProperties iamProperties;

    @Override
    public IssuedToken assumeAgentToken(AgentRegistryEntry agentRegistryEntry) {
        RestClient restClient = restClientBuilder.baseUrl(iamProperties.getBaseUrl()).build();
        TokenResponse response = restClient.post()
                .uri("/iam/projects/{proxyProjectId}/assume_agent_token", iamProperties.getProxyProjectId())
                .body(new AssumeAgentTokenRequest(
                        new AssumeAgentTokenData(
                                "assume_agent_token",
                                new AssumeAgentTokenAttributes(
                                        agentRegistryEntry.agentServiceAccount(),
                                        agentRegistryEntry.principal(),
                                        agentRegistryEntry.agentId()
                                )
                        )
                ))
                .retrieve()
                .body(TokenResponse.class);
        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "IAM assume_agent_token returned empty access_token");
        }
        return new IssuedToken(response.accessToken(), response.expiresAt());
    }

    private record AssumeAgentTokenRequest(AssumeAgentTokenData data) {
    }

    private record AssumeAgentTokenData(String type, AssumeAgentTokenAttributes attributes) {
    }

    private record AssumeAgentTokenAttributes(String agentServiceAccount, String principal, String agentId) {
        @Override
        @JsonProperty("agent_service_account")
        public String agentServiceAccount() {
            return agentServiceAccount;
        }

        @Override
        @JsonProperty("agent_id")
        public String agentId() {
            return agentId;
        }
    }

    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_at") Instant expiresAt
    ) {
    }
}

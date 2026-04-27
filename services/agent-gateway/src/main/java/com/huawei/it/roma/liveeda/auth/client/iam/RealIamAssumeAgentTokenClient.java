package com.huawei.it.roma.liveeda.auth.client.iam;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.huawei.it.roma.liveeda.auth.config.IamProperties;
import com.huawei.it.roma.liveeda.auth.domain.AgentRegistryEntry;
import com.huawei.it.roma.liveeda.auth.domain.IssuedToken;
import com.huawei.it.roma.liveeda.auth.web.GatewayException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
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
        String delegatorAppId = agentRegistryEntry.appId();
        String delegatorAccountName = "Agent_" + delegatorAppId;
        TokenResponse response = restClient.post()
                .uri("/iam/projects/{proxyProjectId}/assume_agent_token", iamProperties.getProxyProjectId())
                // TODO: switch this placeholder header to the IAM SDK result after the real delegation flow is wired in.
                .header(HttpHeaders.AUTHORIZATION, iamProperties.getAuthorizationHeader())
                .body(new AssumeAgentTokenRequest(
                        new AssumeAgentTokenData(
                                "assume_agent_token",
                                new AssumeAgentTokenAttributes(
                                        delegatorAccountName,
                                        delegatorAppId,
                                        agentRegistryEntry.agentId()
                                )
                        )
                ))
                .retrieve()
                .body(TokenResponse.class);
        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "IAM assume_agent_token returned empty access_token");
        }
        Instant expiresAt = response.resolveExpiresAt();
        if (expiresAt == null) {
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "IAM assume_agent_token returned empty expires_at");
        }
        return new IssuedToken(response.accessToken(), expiresAt);
    }

    private record AssumeAgentTokenRequest(AssumeAgentTokenData data) {
    }

    private record AssumeAgentTokenData(String type, AssumeAgentTokenAttributes attributes) {
    }

    private record AssumeAgentTokenAttributes(String delegatorAccountName, String delegatorAppid, String agentId) {
        @Override
        @JsonProperty("delegator_account_name")
        public String delegatorAccountName() {
            return delegatorAccountName;
        }

        @Override
        @JsonProperty("delegator_appid")
        public String delegatorAppid() {
            return delegatorAppid;
        }

        @Override
        @JsonProperty("agent_id")
        public String agentId() {
            return agentId;
        }
    }

    private record TokenResponse(
            String message,
            String code,
            String enterprise,
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_at") Instant expiresAt,
            @JsonProperty("expires_in") Long expiresIn,
            @JsonProperty("token_id") String tokenId,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_on") Long expiresOn
    ) {
        private Instant resolveExpiresAt() {
            if (expiresAt != null) {
                return expiresAt;
            }
            return expiresOn == null ? null : Instant.ofEpochMilli(expiresOn);
        }
    }
}

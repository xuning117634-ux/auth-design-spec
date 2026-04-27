package com.huawei.it.roma.liveeda.auth.client.iam;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.huawei.it.roma.liveeda.auth.config.IamProperties;
import com.huawei.it.roma.liveeda.auth.domain.IssuedToken;
import com.huawei.it.roma.liveeda.auth.web.GatewayException;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Profile("real")
@RequiredArgsConstructor
public class RealIamGatewayTokenClient implements IamGatewayTokenClient {

    private final RestClient.Builder restClientBuilder;
    private final IamProperties iamProperties;
    private final Clock clock;

    private IssuedToken cachedToken;

    @Override
    public synchronized IssuedToken getGatewayAgentToken() {
        if (isCachedTokenUsable()) {
            return cachedToken;
        }
        cachedToken = requestGatewayAgentToken();
        return cachedToken;
    }

    private boolean isCachedTokenUsable() {
        if (cachedToken == null || cachedToken.expiresAt() == null) {
            return false;
        }
        Instant refreshAt = clock.instant().plusSeconds(iamProperties.getGatewayTokenRefreshSkewSeconds());
        return cachedToken.expiresAt().isAfter(refreshAt);
    }

    private IssuedToken requestGatewayAgentToken() {
        RestClient restClient = restClientBuilder.baseUrl(iamProperties.getBaseUrl()).build();
        TokenResponse response = restClient.post()
                .uri(iamProperties.getAgentTokenPath())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AgentTokenRequest(new AgentTokenData(
                        "agent_token",
                        new AgentTokenAttributes(
                                iamProperties.getGatewayAccount(),
                                iamProperties.getGatewaySecret(),
                                iamProperties.getGatewayProject(),
                                iamProperties.getGatewayEnterprise()
                        )
                )))
                .retrieve()
                .body(TokenResponse.class);
        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "IAM agent-token returned empty access_token");
        }
        Instant expiresAt = response.resolveExpiresAt(clock);
        if (expiresAt == null) {
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "IAM agent-token returned empty expires_at");
        }
        return new IssuedToken(response.accessToken(), expiresAt);
    }

    private record AgentTokenRequest(AgentTokenData data) {
    }

    private record AgentTokenData(String type, AgentTokenAttributes attributes) {
    }

    private record AgentTokenAttributes(String account, String secret, String project, String enterprise) {
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
        private Instant resolveExpiresAt(Clock clock) {
            if (expiresAt != null) {
                return expiresAt;
            }
            if (expiresOn != null) {
                return Instant.ofEpochMilli(expiresOn);
            }
            return expiresIn == null ? null : clock.instant().plusSeconds(expiresIn);
        }
    }
}

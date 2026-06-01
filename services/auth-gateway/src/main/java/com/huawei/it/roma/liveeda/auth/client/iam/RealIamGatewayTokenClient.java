package com.huawei.it.roma.liveeda.auth.client.iam;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.huawei.it.roma.liveeda.auth.config.IamProperties;
import com.huawei.it.roma.liveeda.auth.domain.IssuedToken;
import com.huawei.it.roma.liveeda.auth.util.LogSanitizer;
import com.huawei.it.roma.liveeda.auth.web.GatewayException;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Profile("real")
@RequiredArgsConstructor
@Slf4j
public class RealIamGatewayTokenClient implements IamGatewayTokenClient {

    private final RestClient.Builder restClientBuilder;
    private final IamProperties iamProperties;
    private final Clock clock;

    private IssuedToken cachedToken;

    @Override
    public synchronized IssuedToken getGatewayAgentToken() {
        if (isCachedTokenUsable()) {
            log.info("iam gateway agent token cache hit, tokenPresent={}, expiresAt={}",
                    LogSanitizer.present(cachedToken.accessToken()), cachedToken.expiresAt());
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
        long startNanos = System.nanoTime();
        log.info("iam request started, operation=agent-token, baseUrl={}, path={}, gatewayAccount={}, gatewayProject={}",
                iamProperties.getBaseUrl(), iamProperties.getAgentTokenPath(),
                iamProperties.getGatewayAccount(), iamProperties.getGatewayProject());
        TokenResponse response;
        try {
            response = restClient.post()
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
        } catch (RuntimeException exception) {
            log.error("iam request failed, operation=agent-token, elapsedMs={}, error={}",
                    LogSanitizer.elapsedMillis(startNanos), exception.getClass().getSimpleName(), exception);
            throw exception;
        }
        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            log.warn("iam agent-token returned empty access token, elapsedMs={}",
                    LogSanitizer.elapsedMillis(startNanos));
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "IAM agent-token returned empty access_token");
        }
        Instant expiresAt = response.resolveExpiresAt(clock);
        if (expiresAt == null) {
            log.warn("iam agent-token returned empty expires_at, tokenPresent={}, elapsedMs={}",
                    LogSanitizer.present(response.accessToken()), LogSanitizer.elapsedMillis(startNanos));
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "IAM agent-token returned empty expires_at");
        }
        log.info("iam request completed, operation=agent-token, tokenPresent={}, expiresAt={}, elapsedMs={}",
                LogSanitizer.present(response.accessToken()), expiresAt, LogSanitizer.elapsedMillis(startNanos));
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

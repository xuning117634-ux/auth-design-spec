package com.huawei.it.roma.liveeda.auth.client.iam;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.huawei.it.roma.liveeda.auth.config.IamProperties;
import com.huawei.it.roma.liveeda.auth.domain.AgentRegistryEntry;
import com.huawei.it.roma.liveeda.auth.domain.IssuedToken;
import com.huawei.it.roma.liveeda.auth.util.LogSanitizer;
import com.huawei.it.roma.liveeda.auth.web.GatewayException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Profile("real")
@RequiredArgsConstructor
@Slf4j
public class RealIamAssumeAgentTokenClient implements IamAssumeAgentTokenClient {

    private final RestClient.Builder restClientBuilder;
    private final IamProperties iamProperties;
    private final IamGatewayTokenClient iamGatewayTokenClient;

    @Override
    public IssuedToken assumeAgentToken(AgentRegistryEntry agentRegistryEntry) {
        RestClient restClient = restClientBuilder.baseUrl(iamProperties.getBaseUrl()).build();
        IssuedToken gatewayAgentToken = iamGatewayTokenClient.getGatewayAgentToken();
        String delegatorAppId = agentRegistryEntry.appId();
        String delegatorAccountName = "Agent_" + agentRegistryEntry.agentId();
        long startNanos = System.nanoTime();
        log.info("iam request started, operation=assume-agent-token, baseUrl={}, path={}, agentId={}, appId={}, gatewayProject={}, gatewayTokenPresent={}",
                iamProperties.getBaseUrl(), iamProperties.getAssumeAgentTokenPath(),
                agentRegistryEntry.agentId(), delegatorAppId, iamProperties.getGatewayProject(),
                LogSanitizer.present(gatewayAgentToken.accessToken()));
        TokenResponse response;
        try {
            response = restClient.post()
                    .uri(iamProperties.getAssumeAgentTokenPath(), iamProperties.getGatewayProject())
                    .header(HttpHeaders.AUTHORIZATION, gatewayAgentToken.accessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new AssumeAgentTokenRequest(
                            new AssumeAgentTokenData(
                                    "assume_agent_token",
                                    new AssumeAgentTokenAttributes(
                                            delegatorAccountName,
                                            delegatorAppId
                                    )
                            )
                    ))
                    .retrieve()
                    .body(TokenResponse.class);
        } catch (RuntimeException exception) {
            log.error("iam request failed, operation=assume-agent-token, agentId={}, elapsedMs={}, error={}",
                    agentRegistryEntry.agentId(), LogSanitizer.elapsedMillis(startNanos),
                    exception.getClass().getSimpleName(), exception);
            throw exception;
        }
        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            log.warn("iam assume-agent-token returned empty access token, agentId={}, elapsedMs={}",
                    agentRegistryEntry.agentId(), LogSanitizer.elapsedMillis(startNanos));
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "IAM assume_agent_token returned empty access_token");
        }
        Instant expiresAt = response.resolveExpiresAt();
        if (expiresAt == null) {
            log.warn("iam assume-agent-token returned empty expires_at, agentId={}, tokenPresent={}, elapsedMs={}",
                    agentRegistryEntry.agentId(), LogSanitizer.present(response.accessToken()),
                    LogSanitizer.elapsedMillis(startNanos));
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "IAM assume_agent_token returned empty expires_at");
        }
        log.info("iam request completed, operation=assume-agent-token, agentId={}, tokenPresent={}, expiresAt={}, elapsedMs={}",
                agentRegistryEntry.agentId(), LogSanitizer.present(response.accessToken()), expiresAt,
                LogSanitizer.elapsedMillis(startNanos));
        return new IssuedToken(response.accessToken(), expiresAt);
    }

    private record AssumeAgentTokenRequest(AssumeAgentTokenData data) {
    }

    private record AssumeAgentTokenData(String type, AssumeAgentTokenAttributes attributes) {
    }

    private record AssumeAgentTokenAttributes(String delegatorAccountName, String delegatorAppid) {
        @Override
        @JsonProperty("principal_account_name")
        public String delegatorAccountName() {
            return delegatorAccountName;
        }

        @Override
        @JsonProperty("principal_appid")
        public String delegatorAppid() {
            return delegatorAppid;
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

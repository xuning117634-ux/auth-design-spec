package com.huawei.it.roma.liveeda.auth.client.iam;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.huawei.it.roma.liveeda.auth.config.IamProperties;
import com.huawei.it.roma.liveeda.auth.domain.AgentRegistryEntry;
import com.huawei.it.roma.liveeda.auth.domain.IssuedToken;
import com.huawei.it.roma.liveeda.auth.domain.UserAuthorizationResult;
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
public class RealIamResourceTokenClient implements IamResourceTokenClient {

    private final RestClient.Builder restClientBuilder;
    private final IamProperties iamProperties;

    @Override
    public IssuedToken issueResourceToken(
            AgentRegistryEntry agentRegistryEntry,
            UserAuthorizationResult userAuthorizationResult,
            IssuedToken agentToken
    ) {
        RestClient restClient = restClientBuilder.baseUrl(iamProperties.getBaseUrl()).build();
        long startNanos = System.nanoTime();
        log.info("iam request started, operation=resource-token, baseUrl={}, path={}, agentId={}, userId={}, permissionPoints={}, agentTokenPresent={}, userTokenPresent={}",
                iamProperties.getBaseUrl(), iamProperties.getResourceTokenPath(),
                agentRegistryEntry.agentId(), userAuthorizationResult.userId(),
                LogSanitizer.size(userAuthorizationResult.authorizedPermissionPointCodes()),
                LogSanitizer.present(agentToken.accessToken()),
                LogSanitizer.present(userAuthorizationResult.accessToken()));
        TokenResponse response;
        try {
            response = restClient.post()
                    .uri(iamProperties.getResourceTokenPath())
                    .header(HttpHeaders.AUTHORIZATION, agentToken.accessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ResourceTokenRequest(new ResourceTokenData(
                            "resource_token",
                            new ResourceTokenAttributes(userAuthorizationResult.accessToken())
                    )))
                    .retrieve()
                    .body(TokenResponse.class);
        } catch (RuntimeException exception) {
            log.error("iam request failed, operation=resource-token, agentId={}, userId={}, elapsedMs={}, error={}",
                    agentRegistryEntry.agentId(), userAuthorizationResult.userId(),
                    LogSanitizer.elapsedMillis(startNanos), exception.getClass().getSimpleName(), exception);
            throw exception;
        }
        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            log.warn("iam resource-token returned empty access token, agentId={}, userId={}, elapsedMs={}",
                    agentRegistryEntry.agentId(), userAuthorizationResult.userId(),
                    LogSanitizer.elapsedMillis(startNanos));
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "IAM resource-token returned empty access_token");
        }
        Instant expiresAt = response.resolveExpiresAt();
        if (expiresAt == null) {
            log.warn("iam resource-token returned empty expires_at, agentId={}, userId={}, tokenPresent={}, elapsedMs={}",
                    agentRegistryEntry.agentId(), userAuthorizationResult.userId(),
                    LogSanitizer.present(response.accessToken()), LogSanitizer.elapsedMillis(startNanos));
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "IAM resource-token returned empty expires_at");
        }
        log.info("iam request completed, operation=resource-token, agentId={}, userId={}, tokenPresent={}, expiresAt={}, elapsedMs={}",
                agentRegistryEntry.agentId(), userAuthorizationResult.userId(),
                LogSanitizer.present(response.accessToken()), expiresAt, LogSanitizer.elapsedMillis(startNanos));
        return new IssuedToken(response.accessToken(), expiresAt);
    }

    private record ResourceTokenRequest(ResourceTokenData data) {
    }

    private record ResourceTokenData(String type, ResourceTokenAttributes attributes) {
    }

    private record ResourceTokenAttributes(String userToken) {
        @Override
        @JsonProperty("user_token")
        public String userToken() {
            return userToken;
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
            @JsonProperty("refresh_token") String refreshToken,
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

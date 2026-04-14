package com.huawei.it.roma.liveeda.auth.client.iam;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.huawei.it.roma.liveeda.auth.config.IamProperties;
import com.huawei.it.roma.liveeda.auth.domain.AgentRegistryEntry;
import com.huawei.it.roma.liveeda.auth.domain.IssuedToken;
import com.huawei.it.roma.liveeda.auth.domain.UserAuthorizationResult;
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
        TokenResponse response = restClient.post()
                .uri("/iam/auth/resource-token")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentToken.accessToken())
                .body(new ResourceTokenRequest(new ResourceTokenData(
                        "resource_token",
                        new ResourceTokenAttributes(userAuthorizationResult.accessToken())
                )))
                .retrieve()
                .body(TokenResponse.class);
        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "IAM resource-token returned empty access_token");
        }
        return new IssuedToken(response.accessToken(), response.expiresAt());
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
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_at") Instant expiresAt
    ) {
    }
}

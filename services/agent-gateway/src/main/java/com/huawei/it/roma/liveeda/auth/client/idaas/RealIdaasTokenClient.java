package com.huawei.it.roma.liveeda.auth.client.idaas;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.huawei.it.roma.liveeda.auth.config.AgentGatewayProperties;
import com.huawei.it.roma.liveeda.auth.config.IdaasProperties;
import com.huawei.it.roma.liveeda.auth.domain.BaseLoginResult;
import com.huawei.it.roma.liveeda.auth.domain.UserAuthorizationResult;
import com.huawei.it.roma.liveeda.auth.web.GatewayException;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Profile("real")
@RequiredArgsConstructor
public class RealIdaasTokenClient implements IdaasTokenClient {

    private final RestClient.Builder restClientBuilder;
    private final IdaasProperties idaaSProperties;
    private final AgentGatewayProperties properties;

    @Override
    public BaseLoginResult exchangeBaseLoginCode(String code) {
        TokenResponse response = exchangeToken(code, properties.getSelfBaseUrl() + "/gw/auth/base/callback");
        DecodedJWT decodedJWT = JWT.decode(response.accessToken());
        Map<String, Object> userClaim = decodedJWT.getClaim("user").asMap();
        String userId = stringOrDefault(userClaim, "user_id", decodedJWT.getSubject());
        String username = stringOrDefault(userClaim, "username", userId);
        return new BaseLoginResult(userId, username);
    }

    @Override
    public UserAuthorizationResult exchangeConsentCode(String code) {
        TokenResponse response = exchangeToken(code, properties.getSelfBaseUrl() + "/gw/auth/consent/callback");
        DecodedJWT decodedJWT = JWT.decode(response.accessToken());
        Map<String, Object> userClaim = decodedJWT.getClaim("user").asMap();
        String userId = stringOrDefault(userClaim, "user_id", decodedJWT.getSubject());
        String username = stringOrDefault(userClaim, "username", userId);
        Set<String> scopes = new LinkedHashSet<>(decodedJWT.getClaim("consented_scopes").asList(String.class));
        return new UserAuthorizationResult(userId, username, scopes, response.accessToken(), decodedJWT.getExpiresAtAsInstant());
    }

    private TokenResponse exchangeToken(String code, String redirectUri) {
        RestClient restClient = restClientBuilder.baseUrl(idaaSProperties.getTokenUrl()).build();
        TokenResponse response = restClient.post()
                .body(new TokenRequest("authorization_code", code, idaaSProperties.getClientId(), redirectUri))
                .retrieve()
                .body(TokenResponse.class);
        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "IDaaS token endpoint returned an empty access_token");
        }
        return response;
    }

    private String stringOrDefault(Map<String, Object> map, String key, String fallback) {
        if (map == null) {
            return fallback;
        }
        Object value = map.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private record TokenRequest(String grantType, String code, String clientId, String redirectUri) {
        @Override
        @JsonProperty("grant_type")
        public String grantType() {
            return grantType;
        }

        @Override
        @JsonProperty("client_id")
        public String clientId() {
            return clientId;
        }

        @Override
        @JsonProperty("redirect_uri")
        public String redirectUri() {
            return redirectUri;
        }
    }

    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_at") Instant expiresAt
    ) {
    }
}

package com.huawei.it.roma.liveeda.auth.client.idaas;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.huawei.it.roma.liveeda.auth.config.IdaasProperties;
import com.huawei.it.roma.liveeda.auth.domain.AuthorizedPermissionPoint;
import com.huawei.it.roma.liveeda.auth.domain.BaseLoginResult;
import com.huawei.it.roma.liveeda.auth.domain.IssuedToken;
import com.huawei.it.roma.liveeda.auth.domain.UserAuthorizationResult;
import com.huawei.it.roma.liveeda.auth.web.GatewayException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Profile("real")
@RequiredArgsConstructor
public class RealIdaasTokenClient implements IdaasTokenClient {

    private final RestClient.Builder restClientBuilder;
    private final IdaasProperties idaaSProperties;

    @Override
    public IssuedToken exchangeAuthorizationCode(String code, String redirectUri) {
        TokenResponse response = exchangeToken(code, redirectUri);
        Instant expiresAt = response.expiresAt();
        if (expiresAt == null && response.expiresIn() != null) {
            expiresAt = Instant.now().plusSeconds(response.expiresIn());
        }
        if (expiresAt == null) {
            expiresAt = JWT.decode(response.accessToken()).getExpiresAtAsInstant();
        }
        return new IssuedToken(response.accessToken(), expiresAt);
    }

    @Override
    public BaseLoginResult fetchUserInfo(String accessToken) {
        RestClient restClient = restClientBuilder.baseUrl(idaaSProperties.getUserinfoUrl()).build();
        UserInfoResponse response = restClient.get()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(UserInfoResponse.class);
        if (response == null || response.userId() == null || response.userId().isBlank()) {
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "IDaaS userinfo endpoint returned empty user_id");
        }
        String uuid = response.uuid() == null || response.uuid().isBlank() ? response.userId() : response.uuid();
        String username = response.username() == null || response.username().isBlank()
                ? response.userId()
                : response.username();
        return new BaseLoginResult(response.userId(), uuid, username);
    }

    @Override
    public UserAuthorizationResult fetchAuthorizationResult(IssuedToken tc) {
        DecodedJWT decodedJWT = JWT.decode(tc.accessToken());
        BaseLoginResult userInfo = fetchUserInfo(tc.accessToken());
        List<AuthorizedPermissionPoint> permissionPoints = extractConsentedScopes(decodedJWT);
        if (permissionPoints.isEmpty()) {
            throw new GatewayException(HttpStatus.BAD_GATEWAY,
                    "IDaaS token response missing consented_scopes");
        }
        Set<String> permissionPointCodes = permissionPoints.stream()
                .map(AuthorizedPermissionPoint::code)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return new UserAuthorizationResult(
                userInfo.userId(),
                userInfo.username(),
                permissionPointCodes,
                permissionPoints,
                tc.accessToken(),
                tc.expiresAt()
        );
    }

    private List<AuthorizedPermissionPoint> extractConsentedScopes(DecodedJWT decodedJWT) {
        List<Map<?, ?>> claims = readMapClaims(decodedJWT, "consented_scopes");
        List<AuthorizedPermissionPoint> permissionPoints = new ArrayList<>();
        if (claims == null) {
            return permissionPoints;
        }
        for (Map<?, ?> claim : claims) {
            String code = claim == null ? null : String.valueOf(claim.get("code"));
            String displayNameZh = claim == null ? null : String.valueOf(claim.get("displayNameZh"));
            if (code != null && !code.isBlank()) {
                permissionPoints.add(new AuthorizedPermissionPoint(code, displayNameZh == null ? code : displayNameZh));
            }
        }
        return permissionPoints;
    }

    private List<Map<?, ?>> readMapClaims(DecodedJWT decodedJWT, String claimName) {
        List<Object> rawClaims = decodedJWT.getClaim(claimName).asList(Object.class);
        if (rawClaims == null) {
            return null;
        }
        List<Map<?, ?>> claims = new ArrayList<>();
        for (Object rawClaim : rawClaims) {
            if (rawClaim instanceof Map<?, ?> map) {
                claims.add(map);
            }
        }
        return claims;
    }

    private TokenResponse exchangeToken(String code, String redirectUri) {
        RestClient restClient = restClientBuilder.baseUrl(idaaSProperties.getTokenUrl()).build();
        TokenResponse response = restClient.post()
                .body(new TokenRequest(
                        "authorization_code",
                        code,
                        idaaSProperties.getClientId(),
                        idaaSProperties.getClientSecret(),
                        redirectUri
                ))
                .retrieve()
                .body(TokenResponse.class);
        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "IDaaS token endpoint returned an empty access_token");
        }
        return response;
    }

    private record TokenRequest(String grantType, String code, String clientId, String clientSecret, String redirectUri) {
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
        @JsonProperty("client_secret")
        public String clientSecret() {
            return clientSecret;
        }

        @Override
        @JsonProperty("redirect_uri")
        public String redirectUri() {
            return redirectUri;
        }
    }

    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_at") Instant expiresAt,
            @JsonProperty("expires_in") Long expiresIn
    ) {
    }

    private record UserInfoResponse(
            @JsonProperty("user_id") String userId,
            String uuid,
            String username
    ) {
    }
}

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
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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
        if (response == null || response.uid() == null || response.uid().isBlank()) {
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "IDaaS userinfo endpoint returned empty uid");
        }
        String uuid = firstNonBlank(response.uuid(), response.uid());
        String username = firstNonBlank(
                response.displayNameCn(),
                response.displayName(),
                response.displayNameEn(),
                response.uid()
        );
        return new BaseLoginResult(response.uid(), uuid, username);
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
        List<String> claims = decodedJWT.getClaim("consented_scopes").asList(String.class);
        List<AuthorizedPermissionPoint> permissionPoints = new ArrayList<>();
        if (claims == null) {
            return permissionPoints;
        }
        for (String claim : claims) {
            String code = stringValue(claim);
            if (code != null && !code.isBlank()) {
                permissionPoints.add(new AuthorizedPermissionPoint(code, code));
            }
        }
        return permissionPoints;
    }

    private TokenResponse exchangeToken(String code, String redirectUri) {
        RestClient restClient = restClientBuilder.baseUrl(idaaSProperties.getTokenUrl()).build();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", redirectUri);

        TokenResponse response = restClient.post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .headers(headers -> headers.setBasicAuth(
                        idaaSProperties.getClientId(),
                        idaaSProperties.getClientSecret()
                ))
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "IDaaS token endpoint returned an empty access_token");
        }
        return response;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_at") Instant expiresAt,
            @JsonProperty("expires_in") Long expiresIn,
            @JsonProperty("refresh_token") String refreshToken
    ) {
    }

    private record UserInfoResponse(
            String uid,
            String uuid,
            String displayNameCn,
            String displayName,
            String displayNameEn
    ) {
    }
}

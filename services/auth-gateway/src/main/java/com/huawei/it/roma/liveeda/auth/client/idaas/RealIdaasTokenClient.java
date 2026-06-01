package com.huawei.it.roma.liveeda.auth.client.idaas;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.huawei.it.roma.liveeda.auth.config.IdaasProperties;
import com.huawei.it.roma.liveeda.auth.domain.AuthorizedPermissionPoint;
import com.huawei.it.roma.liveeda.auth.domain.BaseLoginResult;
import com.huawei.it.roma.liveeda.auth.domain.IssuedToken;
import com.huawei.it.roma.liveeda.auth.domain.UserAuthorizationResult;
import com.huawei.it.roma.liveeda.auth.util.LogSanitizer;
import com.huawei.it.roma.liveeda.auth.web.GatewayException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
        long startNanos = System.nanoTime();
        log.info("idaas userinfo request started, tokenPresent={}", LogSanitizer.present(accessToken));
        UserInfoResponse response;
        try {
            response = restClient.get()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(UserInfoResponse.class);
        } catch (RuntimeException exception) {
            log.error("idaas userinfo request failed, elapsedMs={}, error={}",
                    LogSanitizer.elapsedMillis(startNanos), exception.getClass().getSimpleName(), exception);
            throw exception;
        }
        if (response == null || response.uid() == null || response.uid().isBlank()) {
            log.warn("idaas userinfo returned empty uid, elapsedMs={}", LogSanitizer.elapsedMillis(startNanos));
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "IDaaS userinfo endpoint returned empty uid");
        }
        String uuid = firstNonBlank(response.uuid(), response.uid());
        String username = firstNonBlank(
                response.displayNameCn(),
                response.displayName(),
                response.displayNameEn(),
                response.uid()
        );
        log.info("idaas userinfo request completed, userId={}, uuidPresent={}, usernamePresent={}, elapsedMs={}",
                response.uid(), LogSanitizer.present(uuid), LogSanitizer.present(username),
                LogSanitizer.elapsedMillis(startNanos));
        return new BaseLoginResult(response.uid(), uuid, username);
    }

    @Override
    public UserAuthorizationResult fetchAuthorizationResult(IssuedToken tc) {
        DecodedJWT decodedJWT = JWT.decode(tc.accessToken());
        BaseLoginResult userInfo = fetchUserInfo(tc.accessToken());
        List<AuthorizedPermissionPoint> permissionPoints = extractConsentedScopes(decodedJWT);
        if (permissionPoints.isEmpty()) {
            log.warn("idaas authorization result missing consented scopes, userId={}", userInfo.userId());
            throw new GatewayException(HttpStatus.BAD_GATEWAY,
                    "IDaaS token response missing consented_scopes");
        }
        Set<String> permissionPointCodes = permissionPoints.stream()
                .map(AuthorizedPermissionPoint::code)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        log.info("idaas authorization result parsed, userId={}, permissionPoints={}",
                userInfo.userId(), LogSanitizer.size(permissionPointCodes));
        return new UserAuthorizationResult(
                userInfo.userId(),
                userInfo.username(),
                permissionPointCodes,
                permissionPoints,
                tc.accessToken(),
                tc.expiresAt()
        );
    }

    private TokenResponse exchangeToken(String code, String redirectUri) {
        RestClient restClient = restClientBuilder.baseUrl(idaaSProperties.getTokenUrl()).build();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", redirectUri);

        long startNanos = System.nanoTime();
        log.info("idaas token request started, tokenUrl={}, redirectHost={}, codePresent={}",
                idaaSProperties.getTokenUrl(), LogSanitizer.host(redirectUri), LogSanitizer.present(code));

        TokenResponse response;
        try {
            response = restClient.post()
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .headers(headers -> headers.setBasicAuth(
                            idaaSProperties.getClientId(),
                            idaaSProperties.getClientSecret()
                    ))
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
        } catch (RuntimeException exception) {
            log.error("idaas token request failed, redirectHost={}, elapsedMs={}, error={}",
                    LogSanitizer.host(redirectUri), LogSanitizer.elapsedMillis(startNanos),
                    exception.getClass().getSimpleName(), exception);
            throw exception;
        }
        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            log.warn("idaas token endpoint returned empty access token, redirectHost={}, elapsedMs={}",
                    LogSanitizer.host(redirectUri), LogSanitizer.elapsedMillis(startNanos));
            throw new GatewayException(HttpStatus.BAD_GATEWAY, "IDaaS token endpoint returned an empty access_token");
        }
        log.info("idaas token request completed, accessTokenPresent={}, expiresAt={}, expiresIn={}, elapsedMs={}",
                LogSanitizer.present(response.accessToken()), response.expiresAt(), response.expiresIn(),
                LogSanitizer.elapsedMillis(startNanos));
        return response;
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

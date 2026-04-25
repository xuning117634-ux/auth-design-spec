package com.huawei.it.roma.liveeda.auth.client.idaas;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.huawei.it.roma.liveeda.auth.client.PolicyCenterClient;
import com.huawei.it.roma.liveeda.auth.domain.AuthorizedPermissionPoint;
import com.huawei.it.roma.liveeda.auth.domain.BaseLoginResult;
import com.huawei.it.roma.liveeda.auth.domain.IssuedToken;
import com.huawei.it.roma.liveeda.auth.domain.UserAuthorizationResult;
import com.huawei.it.roma.liveeda.auth.store.MockIdaasGrantStore;
import com.huawei.it.roma.liveeda.auth.util.JwtTokenFactory;
import com.huawei.it.roma.liveeda.auth.web.GatewayException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@Profile("mock")
@RequiredArgsConstructor
public class MockIdaasTokenClient implements IdaasTokenClient {

    private final MockIdaasGrantStore mockIdaasGrantStore;
    private final PolicyCenterClient policyCenterClient;
    private final JwtTokenFactory jwtTokenFactory;

    @Override
    public IssuedToken exchangeAuthorizationCode(String code, String redirectUri) {
        MockIdaasGrantStore.MockIdaasGrant grant = mockIdaasGrantStore.consume(code)
                .orElseThrow(() -> new GatewayException(HttpStatus.UNAUTHORIZED, "Invalid mock authorization code"));
        List<AuthorizedPermissionPoint> permissionPoints = "base".equals(grant.flow())
                ? List.of()
                : policyCenterClient.resolveByCodes(grant.permissionPointCodes());
        return jwtTokenFactory.issueMockTc(grant.userId(), grant.username(), permissionPoints);
    }

    @Override
    public BaseLoginResult fetchUserInfo(String accessToken) {
        DecodedJWT decodedJWT = JWT.decode(accessToken);
        Map<String, Object> userClaim = decodedJWT.getClaim("user").asMap();
        String userId = stringOrDefault(userClaim, "user_id", decodedJWT.getSubject());
        String username = stringOrDefault(userClaim, "username", userId);
        return new BaseLoginResult(userId, userId, username);
    }

    @Override
    public UserAuthorizationResult fetchAuthorizationResult(IssuedToken tc) {
        DecodedJWT decodedJWT = JWT.decode(tc.accessToken());
        BaseLoginResult userInfo = fetchUserInfo(tc.accessToken());
        List<AuthorizedPermissionPoint> permissionPoints = extractConsentedScopes(decodedJWT);
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
        List<Object> rawClaims = decodedJWT.getClaim("consented_scopes").asList(Object.class);
        List<AuthorizedPermissionPoint> permissionPoints = new ArrayList<>();
        if (rawClaims == null) {
            return permissionPoints;
        }
        for (Object rawClaim : rawClaims) {
            if (rawClaim instanceof Map<?, ?> claim) {
                String code = claim.get("code") == null ? null : String.valueOf(claim.get("code"));
                String displayNameZh = claim.get("displayNameZh") == null ? code : String.valueOf(claim.get("displayNameZh"));
                if (code != null && !code.isBlank()) {
                    permissionPoints.add(new AuthorizedPermissionPoint(code, displayNameZh));
                }
            }
        }
        return permissionPoints;
    }

    private String stringOrDefault(Map<String, Object> map, String key, String fallback) {
        if (map == null) {
            return fallback;
        }
        Object value = map.get(key);
        return value == null ? fallback : String.valueOf(value);
    }
}

package com.huawei.it.roma.liveeda.auth.client.idaas;

import com.huawei.it.roma.liveeda.auth.client.PolicyCenterClient;
import com.huawei.it.roma.liveeda.auth.domain.BaseLoginResult;
import com.huawei.it.roma.liveeda.auth.domain.IssuedToken;
import com.huawei.it.roma.liveeda.auth.domain.UserAuthorizationResult;
import com.huawei.it.roma.liveeda.auth.store.MockIdaasGrantStore;
import com.huawei.it.roma.liveeda.auth.util.JwtTokenFactory;
import com.huawei.it.roma.liveeda.auth.web.GatewayException;
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
    public BaseLoginResult exchangeBaseLoginCode(String code) {
        MockIdaasGrantStore.MockIdaasGrant grant = mockIdaasGrantStore.consume(code)
                .orElseThrow(() -> new GatewayException(HttpStatus.UNAUTHORIZED, "Invalid mock base login code"));
        if (!"base".equals(grant.flow())) {
            throw new GatewayException(HttpStatus.BAD_REQUEST, "Code does not belong to base login flow");
        }
        return new BaseLoginResult(grant.userId(), grant.username());
    }

    @Override
    public UserAuthorizationResult exchangeConsentCode(String code) {
        MockIdaasGrantStore.MockIdaasGrant grant = mockIdaasGrantStore.consume(code)
                .orElseThrow(() -> new GatewayException(HttpStatus.UNAUTHORIZED, "Invalid mock consent code"));
        if (!"consent".equals(grant.flow())) {
            throw new GatewayException(HttpStatus.BAD_REQUEST, "Code does not belong to consent flow");
        }
        var permissionPoints = policyCenterClient.resolveByCodes(grant.permissionPointCodes());
        IssuedToken tc = jwtTokenFactory.issueMockTc(grant.userId(), grant.username(), permissionPoints);
        return new UserAuthorizationResult(
                grant.userId(),
                grant.username(),
                grant.permissionPointCodes(),
                permissionPoints,
                tc.accessToken(),
                tc.expiresAt()
        );
    }
}

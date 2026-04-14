package com.huawei.it.roma.liveeda.auth.client.idaas;

import com.huawei.it.roma.liveeda.auth.config.AgentGatewayProperties;
import java.net.URI;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Profile("mock")
@RequiredArgsConstructor
public class MockIdaasAuthorizeSupport implements IdaasAuthorizeSupport {

    private final AgentGatewayProperties properties;

    @Override
    public URI buildBaseAuthorizationUri(String gwState) {
        return UriComponentsBuilder.fromHttpUrl(properties.getSelfBaseUrl())
                .path("/mock/idaas/authorize")
                .queryParam("flow", "base")
                .queryParam("redirect_uri", properties.getSelfBaseUrl() + "/gw/auth/base/callback")
                .queryParam("scope", "base")
                .queryParam("state", gwState)
                .build(true)
                .toUri();
    }

    @Override
    public URI buildConsentAuthorizationUri(String gwState, Set<String> requiredPolicyCodes) {
        return UriComponentsBuilder.fromHttpUrl(properties.getSelfBaseUrl())
                .path("/mock/idaas/authorize")
                .queryParam("flow", "consent")
                .queryParam("redirect_uri", properties.getSelfBaseUrl() + "/gw/auth/consent/callback")
                .queryParam("scope", String.join(",", requiredPolicyCodes.stream().sorted().toList()))
                .queryParam("state", gwState)
                .build(true)
                .toUri();
    }
}

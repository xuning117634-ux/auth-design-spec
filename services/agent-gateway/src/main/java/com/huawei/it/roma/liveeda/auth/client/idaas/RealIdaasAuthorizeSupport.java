package com.huawei.it.roma.liveeda.auth.client.idaas;

import com.huawei.it.roma.liveeda.auth.config.AgentGatewayProperties;
import com.huawei.it.roma.liveeda.auth.config.IdaasProperties;
import java.net.URI;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Profile("real")
@RequiredArgsConstructor
public class RealIdaasAuthorizeSupport implements IdaasAuthorizeSupport {

    private final AgentGatewayProperties properties;
    private final IdaasProperties idaaSProperties;

    @Override
    public URI buildBaseAuthorizationUri(String gwState) {
        return UriComponentsBuilder.fromHttpUrl(idaaSProperties.getAuthorizeUrl())
                .queryParam("response_type", "code")
                .queryParam("client_id", idaaSProperties.getClientId())
                .queryParam("redirect_uri", properties.getSelfBaseUrl() + "/gw/auth/base/callback")
                .queryParam("scope", "base")
                .queryParam("state", gwState)
                .build(true)
                .toUri();
    }

    @Override
    public URI buildConsentAuthorizationUri(String gwState, Set<String> requiredPermissionPointCodes) {
        return UriComponentsBuilder.fromHttpUrl(idaaSProperties.getAuthorizeUrl())
                .queryParam("response_type", "code")
                .queryParam("client_id", idaaSProperties.getClientId())
                .queryParam("redirect_uri", properties.getSelfBaseUrl() + "/gw/auth/consent/callback")
                .queryParam("scope", String.join(" ", requiredPermissionPointCodes.stream().sorted().toList()))
                .queryParam("state", gwState)
                .build(true)
                .toUri();
    }
}

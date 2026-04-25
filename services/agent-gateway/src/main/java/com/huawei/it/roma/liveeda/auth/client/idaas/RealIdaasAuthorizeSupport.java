package com.huawei.it.roma.liveeda.auth.client.idaas;

import com.huawei.it.roma.liveeda.auth.config.AgentGatewayProperties;
import com.huawei.it.roma.liveeda.auth.config.IdaasProperties;
import java.net.URI;
import java.util.Map;
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
    public URI buildBaseAuthorizationUri(String agentId, String gwState) {
        return UriComponentsBuilder.fromHttpUrl(idaaSProperties.getAuthorizeUrl())
                .queryParam("response_type", "code")
                .queryParam("client_id", idaaSProperties.getClientId())
                .queryParam("agent_id", agentId)
                .queryParam("redirect_uri", properties.getSelfBaseUrl() + "/gw/auth/base/callback")
                .queryParam("scope", "base")
                .queryParam("state", gwState)
                .build(true)
                .toUri();
    }

    @Override
    public URI buildConsentAuthorizationUri(
            String agentId,
            String gwState,
            Set<String> requiredPermissionPointCodes,
            Map<String, String> subjectHint
    ) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(idaaSProperties.getAuthorizeUrl())
                .queryParam("response_type", "code")
                .queryParam("client_id", idaaSProperties.getClientId())
                .queryParam("agent_id", agentId)
                .queryParam("redirect_uri", properties.getSelfBaseUrl() + "/gw/auth/consent/callback")
                .queryParam("scope", String.join(" ", requiredPermissionPointCodes.stream().sorted().toList()))
                .queryParam("state", gwState);
        String loginHint = resolveLoginHint(subjectHint);
        if (loginHint != null) {
            builder.queryParam("login_hint", loginHint);
        }
        return builder.build(true).toUri();
    }

    private String resolveLoginHint(Map<String, String> subjectHint) {
        if (subjectHint == null || subjectHint.isEmpty()) {
            return null;
        }
        String value = subjectHint.getOrDefault("userId", subjectHint.get("user_id"));
        return value == null || value.isBlank() ? null : value;
    }
}

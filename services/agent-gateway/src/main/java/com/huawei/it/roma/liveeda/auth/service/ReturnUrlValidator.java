package com.huawei.it.roma.liveeda.auth.service;

import com.huawei.it.roma.liveeda.auth.domain.AgentRegistryEntry;
import com.huawei.it.roma.liveeda.auth.web.GatewayException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ReturnUrlValidator {

    public URI validate(AgentRegistryEntry agentRegistryEntry, String returnUrl) {
        URI uri = URI.create(returnUrl);
        if (uri.getHost() == null || agentRegistryEntry.allowedReturnHosts().stream().noneMatch(uri.getHost()::equalsIgnoreCase)) {
            throw new GatewayException(HttpStatus.BAD_REQUEST, "return_url host is not allowed");
        }
        return uri;
    }
}

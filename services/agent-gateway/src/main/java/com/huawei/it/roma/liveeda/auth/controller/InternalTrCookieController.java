package com.huawei.it.roma.liveeda.auth.controller;

import com.huawei.it.roma.liveeda.auth.config.AgentGatewayProperties;
import com.huawei.it.roma.liveeda.auth.service.ResourceCookieService;
import com.huawei.it.roma.liveeda.auth.web.GatewayException;
import com.huawei.it.roma.liveeda.auth.web.TrCookieResolveRequest;
import com.huawei.it.roma.liveeda.auth.web.TrCookieResolveResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/tr-cookie")
@RequiredArgsConstructor
public class InternalTrCookieController {

    private final ResourceCookieService resourceCookieService;
    private final AgentGatewayProperties properties;

    @PostMapping("/resolve")
    public TrCookieResolveResponse resolve(
            @RequestHeader HttpHeaders headers,
            @Valid @RequestBody TrCookieResolveRequest request
    ) {
        validateInternalAccess(headers);
        return resourceCookieService.resolve(request);
    }

    private void validateInternalAccess(HttpHeaders headers) {
        String expectedValue = properties.getTrCookieResolveAuthHeaderValue();
        if (expectedValue == null || expectedValue.isBlank()) {
            return;
        }
        String headerName = properties.getTrCookieResolveAuthHeaderName();
        String actualValue = headers.getFirst(headerName);
        if (!expectedValue.equals(actualValue)) {
            throw new GatewayException(HttpStatus.UNAUTHORIZED, "Invalid tr-cookie resolve credential");
        }
    }
}

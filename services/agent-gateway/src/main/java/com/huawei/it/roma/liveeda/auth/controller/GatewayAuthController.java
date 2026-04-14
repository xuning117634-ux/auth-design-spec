package com.huawei.it.roma.liveeda.auth.controller;

import com.huawei.it.roma.liveeda.auth.config.AgentGatewayProperties;
import com.huawei.it.roma.liveeda.auth.service.GatewayAuthService;
import com.huawei.it.roma.liveeda.auth.web.GatewayException;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/gw/auth")
@RequiredArgsConstructor
public class GatewayAuthController {

    private final GatewayAuthService gatewayAuthService;
    private final AgentGatewayProperties properties;

    @GetMapping("/login")
    public ResponseEntity<Void> startLogin(
            @RequestParam("agent_id") @NotBlank String agentId,
            @RequestParam("return_url") @NotBlank String returnUrl,
            @RequestParam("state") @NotBlank String outerState
    ) {
        URI redirectUri = gatewayAuthService.startBaseLogin(agentId, returnUrl, outerState);
        return ResponseEntity.status(HttpStatus.FOUND).location(redirectUri).build();
    }

    @GetMapping("/base/callback")
    public ResponseEntity<Void> handleBaseCallback(
            @RequestParam("code") @NotBlank String code,
            @RequestParam("state") @NotBlank String gwState
    ) {
        GatewayAuthService.BaseLoginCallbackResult callbackResult = gatewayAuthService.handleBaseCallback(code, gwState);
        ResponseCookie responseCookie = ResponseCookie.from(GatewayAuthService.GATEWAY_SESSION_COOKIE, callbackResult.gatewaySessionId())
                .httpOnly(true)
                .secure(properties.isSecureCookies())
                .sameSite("Lax")
                .path("/")
                .build();
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                .location(callbackResult.redirectUri())
                .build();
    }

    @GetMapping("/authorize")
    public ResponseEntity<Void> startConsentAuthorization(
            @RequestParam("request_id") @NotBlank String requestId,
            @CookieValue(name = GatewayAuthService.GATEWAY_SESSION_COOKIE, required = false) String gatewaySessionId
    ) {
        if (gatewaySessionId == null || gatewaySessionId.isBlank()) {
            throw new GatewayException(HttpStatus.UNAUTHORIZED, "Missing gw_session_id cookie");
        }
        URI redirectUri = gatewayAuthService.startConsentAuthorization(requestId, gatewaySessionId);
        return ResponseEntity.status(HttpStatus.FOUND).location(redirectUri).build();
    }

    @GetMapping("/consent/callback")
    public ResponseEntity<Void> handleConsentCallback(
            @RequestParam("code") @NotBlank String code,
            @RequestParam("state") @NotBlank String gwState
    ) {
        URI redirectUri = gatewayAuthService.handleConsentCallback(code, gwState);
        return ResponseEntity.status(HttpStatus.FOUND).location(redirectUri).build();
    }
}

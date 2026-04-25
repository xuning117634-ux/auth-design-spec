package com.huawei.it.roma.liveeda.auth.controller;

import com.huawei.it.roma.liveeda.auth.service.GatewayAuthService;
import com.huawei.it.roma.liveeda.auth.web.LoginTicketExchangeRequest;
import com.huawei.it.roma.liveeda.auth.web.LoginTicketExchangeResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/gw/auth")
@RequiredArgsConstructor
public class GatewayAuthController {

    private final GatewayAuthService gatewayAuthService;

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
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(gatewayAuthService.handleBaseCallback(code, gwState))
                .build();
    }

    @PostMapping("/ticket/exchange")
    public LoginTicketExchangeResponse exchangeLoginTicket(
            @Valid @RequestBody LoginTicketExchangeRequest request
    ) {
        return gatewayAuthService.exchangeLoginTicket(request);
    }

    @GetMapping("/authorize")
    public ResponseEntity<Void> startConsentAuthorization(
            @RequestParam("request_id") @NotBlank String requestId
    ) {
        URI redirectUri = gatewayAuthService.startConsentAuthorization(requestId);
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

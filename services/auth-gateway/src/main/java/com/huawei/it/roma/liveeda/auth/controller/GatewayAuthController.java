package com.huawei.it.roma.liveeda.auth.controller;

import com.huawei.it.roma.liveeda.auth.util.LogSanitizer;
import com.huawei.it.roma.liveeda.auth.service.GatewayAuthService;
import com.huawei.it.roma.liveeda.auth.web.LoginTicketExchangeRequest;
import com.huawei.it.roma.liveeda.auth.web.LoginTicketExchangeResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class GatewayAuthController {

    private final GatewayAuthService gatewayAuthService;

    @GetMapping("/login")
    public ResponseEntity<Void> startLogin(
            @RequestParam("agent_id") @NotBlank String agentId,
            @RequestParam("return_url") @NotBlank String returnUrl,
            @RequestParam("state") @NotBlank String outerState
    ) {
        log.info("gw auth login request received, agentId={}, returnHost={}, stateTail={}",
                agentId, LogSanitizer.host(returnUrl), LogSanitizer.tail(outerState));
        URI redirectUri = gatewayAuthService.startBaseLogin(agentId, returnUrl, outerState);
        return ResponseEntity.status(HttpStatus.FOUND).location(redirectUri).build();
    }

    @GetMapping("/base/callback")
    public ResponseEntity<Void> handleBaseCallback(
            @RequestParam("code") @NotBlank String code,
            @RequestParam("state") @NotBlank String gwState
    ) {
        log.info("gw auth base callback received, codePresent={}, gwStateTail={}",
                LogSanitizer.present(code), LogSanitizer.tail(gwState));
        URI redirectUri = gatewayAuthService.handleBaseCallback(code, gwState);
        return ResponseEntity.status(HttpStatus.FOUND).location(redirectUri).build();
    }

    @PostMapping("/ticket/exchange")
    public LoginTicketExchangeResponse exchangeLoginTicket(
            @Valid @RequestBody LoginTicketExchangeRequest request
    ) {
        log.info("gw auth ticket exchange request received, agentId={}, ticketTail={}",
                request.agentId(), LogSanitizer.tail(request.ticketST()));
        return gatewayAuthService.exchangeLoginTicket(request);
    }

    @GetMapping("/authorize")
    public ResponseEntity<Void> startConsentAuthorization(
            @RequestParam("request_id") @NotBlank String requestId
    ) {
        log.info("gw auth consent authorization request received, requestId={}", requestId);
        URI redirectUri = gatewayAuthService.startConsentAuthorization(requestId);
        return ResponseEntity.status(HttpStatus.FOUND).location(redirectUri).build();
    }

    @GetMapping("/consent/callback")
    public ResponseEntity<Void> handleConsentCallback(
            @RequestParam("code") @NotBlank String code,
            @RequestParam("state") @NotBlank String gwState
    ) {
        log.info("gw auth consent callback received, codePresent={}, gwStateTail={}",
                LogSanitizer.present(code), LogSanitizer.tail(gwState));
        URI redirectUri = gatewayAuthService.handleConsentCallback(code, gwState);
        return ResponseEntity.status(HttpStatus.FOUND).location(redirectUri).build();
    }
}

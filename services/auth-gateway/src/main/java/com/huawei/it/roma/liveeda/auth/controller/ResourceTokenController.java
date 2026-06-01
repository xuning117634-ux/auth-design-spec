package com.huawei.it.roma.liveeda.auth.controller;

import com.huawei.it.roma.liveeda.auth.util.LogSanitizer;
import com.huawei.it.roma.liveeda.auth.service.GatewayAuthService;
import com.huawei.it.roma.liveeda.auth.service.ResourceTokenService;
import com.huawei.it.roma.liveeda.auth.web.ResourceTokenRequest;
import com.huawei.it.roma.liveeda.auth.web.ResourceTokenResponse;
import com.huawei.it.roma.liveeda.auth.web.TokenResultExchangeRequest;
import com.huawei.it.roma.liveeda.auth.web.TokenResultExchangeResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gw/token")
@RequiredArgsConstructor
@Slf4j
public class ResourceTokenController {

    private final ResourceTokenService resourceTokenService;
    private final GatewayAuthService gatewayAuthService;

    @PostMapping("/resource-token")
    public ResourceTokenResponse issueResourceToken(
            @Valid @RequestBody ResourceTokenRequest request
    ) {
        log.info("gw resource token request received, agentId={}, requiredTools={}, returnHost={}, stateTail={}, subjectHintKeys={}",
                request.agentId(), LogSanitizer.size(request.requiredTools()), LogSanitizer.host(request.returnUrl()),
                LogSanitizer.tail(request.state()), request.subjectHint() == null ? 0 : request.subjectHint().size());
        return resourceTokenService.issueResourceToken(request);
    }

    @PostMapping("/result/exchange")
    public TokenResultExchangeResponse exchangeTokenResult(
            @Valid @RequestBody TokenResultExchangeRequest request,
            @RequestHeader(name = HttpHeaders.COOKIE, required = false) String cookieHeader
    ) {
        log.info("gw token result exchange request received, agentId={}, requestId={}, ticketTail={}, cookiePresent={}",
                request.agentId(), request.requestId(), LogSanitizer.tail(request.tokenResultTicket()),
                LogSanitizer.present(cookieHeader));
        return gatewayAuthService.exchangeTokenResult(request, cookieHeader);
    }
}

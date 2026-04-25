package com.huawei.it.roma.liveeda.auth.controller;

import com.huawei.it.roma.liveeda.auth.service.GatewayAuthService;
import com.huawei.it.roma.liveeda.auth.service.ResourceTokenService;
import com.huawei.it.roma.liveeda.auth.web.ResourceTokenRequest;
import com.huawei.it.roma.liveeda.auth.web.ResourceTokenResponse;
import com.huawei.it.roma.liveeda.auth.web.TokenResultExchangeRequest;
import com.huawei.it.roma.liveeda.auth.web.TokenResultExchangeResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gw/token")
@RequiredArgsConstructor
public class ResourceTokenController {

    private final ResourceTokenService resourceTokenService;
    private final GatewayAuthService gatewayAuthService;

    @PostMapping("/resource-token")
    public ResourceTokenResponse issueResourceToken(
            @Valid @RequestBody ResourceTokenRequest request
    ) {
        return resourceTokenService.issueResourceToken(request);
    }

    @PostMapping("/result/exchange")
    public TokenResultExchangeResponse exchangeTokenResult(
            @Valid @RequestBody TokenResultExchangeRequest request
    ) {
        return gatewayAuthService.exchangeTokenResult(request);
    }
}

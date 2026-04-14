package com.huawei.it.roma.liveeda.auth.controller;

import com.huawei.it.roma.liveeda.auth.service.ResourceTokenService;
import com.huawei.it.roma.liveeda.auth.web.AuthorizationHeaderUtils;
import com.huawei.it.roma.liveeda.auth.web.ResourceTokenRequest;
import com.huawei.it.roma.liveeda.auth.web.ResourceTokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gw/token")
@RequiredArgsConstructor
public class ResourceTokenController {

    private final ResourceTokenService resourceTokenService;

    @PostMapping("/resource-token")
    public ResourceTokenResponse issueResourceToken(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @Valid @RequestBody ResourceTokenRequest request
    ) {
        return resourceTokenService.issueResourceToken(
                AuthorizationHeaderUtils.extractBearerToken(authorizationHeader),
                request
        );
    }
}

package com.huawei.it.roma.liveeda.demoagent.controller;

import com.huawei.it.roma.liveeda.demoagent.config.DemoAgentProperties;
import com.huawei.it.roma.liveeda.demoagent.domain.SiteSession;
import com.huawei.it.roma.liveeda.demoagent.service.DemoAgentService;
import com.huawei.it.roma.liveeda.demoagent.store.SiteSessionStore;
import java.net.URI;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentEntryController {

    private final DemoAgentProperties properties;
    private final DemoAgentService demoAgentService;
    private final SiteSessionStore siteSessionStore;

    private static final String GATEWAY_SESSION_COOKIE = "gw_session_id";

    @GetMapping("/session")
    public ResponseEntity<Map<String, String>> session(
            @CookieValue(name = DemoAgentService.SITE_SESSION_COOKIE, required = false) String siteSessionId
    ) {
        if (siteSessionId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return siteSessionStore.find(siteSessionId)
                .map(siteSession -> ResponseEntity.ok(Map.of(
                        "userId", siteSession.userId(),
                        "username", siteSession.username()
                )))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = DemoAgentService.SITE_SESSION_COOKIE, required = false) String siteSessionId
    ) {
        demoAgentService.logout(siteSessionId);

        ResponseCookie siteCookie = ResponseCookie.from(DemoAgentService.SITE_SESSION_COOKIE, "")
                .httpOnly(true)
                .secure(properties.isSecureCookies())
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        ResponseCookie gatewayCookie = ResponseCookie.from(GATEWAY_SESSION_COOKIE, "")
                .httpOnly(true)
                .secure(properties.isSecureCookies())
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, siteCookie.toString())
                .header(HttpHeaders.SET_COOKIE, gatewayCookie.toString())
                .build();
    }

    @GetMapping
    public ResponseEntity<Void> enter(
            @RequestParam(name = "gw_session_token", required = false) String gwSessionToken,
            @RequestParam(name = "user_id", required = false) String userId,
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "state", required = false) String state,
            @CookieValue(name = DemoAgentService.SITE_SESSION_COOKIE, required = false) String siteSessionId
    ) {
        if (gwSessionToken != null && userId != null && username != null) {
            SiteSession siteSession = demoAgentService.createSiteSession(gwSessionToken, userId, username);
            ResponseCookie cookie = ResponseCookie.from(DemoAgentService.SITE_SESSION_COOKIE, siteSession.siteSessionId())
                    .httpOnly(true)
                    .secure(properties.isSecureCookies())
                    .sameSite("Lax")
                    .path("/")
                    .build();

            URI redirect = UriComponentsBuilder.fromHttpUrl(properties.getSelfBaseUrl() + "/agent.html")
                    .queryParamIfPresent("state", java.util.Optional.ofNullable(state))
                    .build(true)
                    .toUri();
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .location(redirect)
                    .build();
        }

        if (siteSessionId != null && siteSessionStore.find(siteSessionId).isPresent()) {
            URI redirect = UriComponentsBuilder.fromHttpUrl(properties.getSelfBaseUrl() + "/agent.html")
                    .build(true)
                    .toUri();
            return ResponseEntity.status(HttpStatus.FOUND).location(redirect).build();
        }

        URI loginRedirect = UriComponentsBuilder.fromHttpUrl(properties.getGatewayBaseUrl())
                .path("/gw/auth/login")
                .queryParam("agent_id", properties.getAgentId())
                .queryParam("return_url", properties.getSelfBaseUrl() + "/agent")
                .queryParam("state", "st_login_demo")
                .build(true)
                .toUri();
        return ResponseEntity.status(HttpStatus.FOUND).location(loginRedirect).build();
    }
}

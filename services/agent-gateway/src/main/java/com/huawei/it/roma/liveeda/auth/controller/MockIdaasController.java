package com.huawei.it.roma.liveeda.auth.controller;

import com.huawei.it.roma.liveeda.auth.client.PolicyCenterClient;
import com.huawei.it.roma.liveeda.auth.config.AgentGatewayProperties;
import com.huawei.it.roma.liveeda.auth.domain.AuthorizedPermissionPoint;
import com.huawei.it.roma.liveeda.auth.store.MockIdaasGrantStore;
import com.huawei.it.roma.liveeda.auth.util.IdGenerator;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@Profile("mock")
@RestController
@RequestMapping("/mock/idaas")
@RequiredArgsConstructor
public class MockIdaasController {

    private static final String MOCK_IDAAS_SESSION_COOKIE = "mock_idaas_session";

    private final MockIdaasGrantStore mockIdaasGrantStore;
    private final PolicyCenterClient policyCenterClient;
    private final AgentGatewayProperties properties;
    private final IdGenerator idGenerator;

    @GetMapping(value = "/authorize", produces = MediaType.TEXT_HTML_VALUE)
    public String authorizePage(
            @RequestParam("flow") String flow,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("scope") String scope,
            @RequestParam("state") String state,
            @CookieValue(name = MOCK_IDAAS_SESSION_COOKIE, required = false) String mockIdaasSession
    ) {
        if ("base".equals(flow)) {
            return renderBaseLoginPage(redirectUri, scope, state);
        }
        return renderConsentPage(redirectUri, scope, state, mockIdaasSession);
    }

    @PostMapping("/approve")
    public ResponseEntity<Void> approve(
            @RequestParam("flow") String flow,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("scope") String scope,
            @RequestParam("state") String state,
            @RequestParam(name = "user_id", required = false) String userId,
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "password", required = false) String password,
            @RequestParam(name = "approved", required = false) String approved,
            @CookieValue(name = MOCK_IDAAS_SESSION_COOKIE, required = false) String mockIdaasSession
    ) {
        MockUser mockUser = parseMockUser(mockIdaasSession);
        String defaultUserId = mockUser == null ? properties.getDefaultUserId() : mockUser.userId();
        String defaultUsername = mockUser == null ? properties.getDefaultUsername() : mockUser.username();

        String resolvedUserId = trimToDefault(userId, defaultUserId);
        String resolvedUsername = trimToDefault(username, defaultUsername);
        if ("base".equals(flow)) {
            if (password == null || password.isBlank()) {
                throw new IllegalArgumentException("password must not be blank");
            }
        } else if (!isApproved(approved)) {
            throw new IllegalArgumentException("authorization must be approved");
        }

        String code = idGenerator.next("code");
        Set<String> scopes = "base".equals(flow)
                ? Set.of()
                : Arrays.stream(scope.split(","))
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .collect(Collectors.toSet());
        mockIdaasGrantStore.save(code, new MockIdaasGrantStore.MockIdaasGrant(
                flow,
                resolvedUserId,
                resolvedUsername,
                scopes
        ));

        URI callbackUri = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("code", code)
                .queryParam("state", state)
                .build(true)
                .toUri();
        ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, callbackUri.toString());
        if ("base".equals(flow)) {
            response.header(HttpHeaders.SET_COOKIE, ResponseCookie.from(
                            MOCK_IDAAS_SESSION_COOKIE,
                            encodeMockUser(resolvedUserId, resolvedUsername)
                    )
                    .httpOnly(true)
                    .secure(properties.isSecureCookies())
                    .sameSite("Lax")
                    .path("/mock/idaas")
                    .build()
                    .toString());
        }
        return response.build();
    }

    private String renderBaseLoginPage(String redirectUri, String scope, String state) {
        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                  <meta charset="UTF-8">
                  <title>模拟 IDaaS 登录</title>
                  <style>
                    body { font-family: "Microsoft YaHei", Arial, sans-serif; padding: 32px; background: linear-gradient(135deg, #f3f7ff, #eef8f4); color: #1f2937; }
                    .card { max-width: 720px; margin: 0 auto; background: white; padding: 28px; border-radius: 18px; box-shadow: 0 18px 46px rgba(15,23,42,0.10); }
                    .badge { display: inline-block; background: #e0f2fe; color: #075985; padding: 6px 10px; border-radius: 999px; font-size: 12px; margin-bottom: 14px; }
                    h1 { margin: 0 0 8px; font-size: 28px; }
                    p { line-height: 1.6; }
                    .form-row { margin-top: 16px; }
                    label { display: block; font-weight: 600; margin-bottom: 8px; }
                    input[type=text], input[type=password] { width: 100%%; box-sizing: border-box; border: 1px solid #d1d5db; border-radius: 12px; padding: 12px 14px; font-size: 14px; }
                    .meta { margin-top: 18px; padding: 14px 16px; background: #f8fafc; border: 1px solid #e5e7eb; border-radius: 12px; }
                    .meta code { background: #eef2ff; padding: 2px 6px; border-radius: 6px; }
                    button { margin-top: 20px; background: #145cff; color: white; border: none; border-radius: 10px; padding: 12px 18px; cursor: pointer; font-size: 14px; }
                    .tip { color: #6b7280; font-size: 13px; margin-top: 12px; }
                  </style>
                </head>
                <body>
                  <div class="card">
                    <div class="badge">Mock IDaaS</div>
                    <h1>登录到统一身份认证平台</h1>
                    <p>这是本地联调用的模拟 IDaaS 登录页。为了让演示更接近真实流程，这里保留了账号、显示名和密码输入步骤。</p>
                    <form method="post" action="/mock/idaas/approve">
                      <input type="hidden" name="flow" value="base"/>
                      <input type="hidden" name="redirect_uri" value="%s"/>
                      <input type="hidden" name="scope" value="%s"/>
                      <input type="hidden" name="state" value="%s"/>
                      <div class="form-row">
                        <label for="user_id">账号</label>
                        <input id="user_id" name="user_id" type="text" value="%s" autocomplete="username"/>
                      </div>
                      <div class="form-row">
                        <label for="username">显示名</label>
                        <input id="username" name="username" type="text" value="%s"/>
                      </div>
                      <div class="form-row">
                        <label for="password">密码</label>
                        <input id="password" name="password" type="password" value="MockPassword@123" autocomplete="current-password"/>
                      </div>
                      <div class="meta">
                        <div><strong>当前流程：</strong><code>base</code></div>
                        <div style="margin-top: 8px;"><strong>申请范围：</strong><code>%s</code></div>
                      </div>
                      <button type="submit">登录并继续</button>
                      <div class="tip">这里提交的账号和显示名会进入后续网关会话，密码仅作为本地演示输入项使用。</div>
                    </form>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(redirectUri),
                escapeHtml(scope),
                escapeHtml(state),
                escapeHtml(properties.getDefaultUserId()),
                escapeHtml(properties.getDefaultUsername()),
                escapeHtml(scope)
        );
    }

    private String renderConsentPage(String redirectUri, String scope, String state, String mockIdaasSession) {
        Set<String> permissionPointCodes = Arrays.stream(scope.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toSet());
        String scopeItems = policyCenterClient.resolveByCodes(permissionPointCodes).stream()
                .sorted(Comparator.comparing(AuthorizedPermissionPoint::code))
                .map(point -> """
                        <li><code>%s</code><div style="margin-top: 4px; color: #4b5563;">%s</div></li>
                        """.formatted(escapeHtml(point.code()), escapeHtml(point.displayNameZh())))
                .collect(Collectors.joining());

        MockUser mockUser = parseMockUser(mockIdaasSession);
        String userId = mockUser == null ? properties.getDefaultUserId() : mockUser.userId();
        String username = mockUser == null ? properties.getDefaultUsername() : mockUser.username();

        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                  <meta charset="UTF-8">
                  <title>模拟授权确认</title>
                  <style>
                    body { font-family: "Microsoft YaHei", Arial, sans-serif; padding: 32px; background: linear-gradient(135deg, #fdf7f3, #f7f7ff); color: #1f2937; }
                    .card { max-width: 760px; margin: 0 auto; background: white; padding: 28px; border-radius: 18px; box-shadow: 0 18px 46px rgba(15,23,42,0.10); }
                    .badge { display: inline-block; background: #fef3c7; color: #92400e; padding: 6px 10px; border-radius: 999px; font-size: 12px; margin-bottom: 14px; }
                    h1 { margin: 0 0 8px; font-size: 28px; }
                    p { line-height: 1.6; }
                    ul { margin: 14px 0 0; padding-left: 20px; }
                    li { margin: 10px 0; }
                    code { background: #eef2ff; padding: 2px 6px; border-radius: 6px; }
                    .consent-box { margin-top: 18px; padding: 16px; background: #f8fafc; border: 1px solid #e5e7eb; border-radius: 12px; }
                    .checkbox { display: flex; gap: 10px; align-items: flex-start; margin-top: 16px; }
                    input[type=checkbox] { margin-top: 4px; }
                    button { margin-top: 20px; background: #145cff; color: white; border: none; border-radius: 10px; padding: 12px 18px; cursor: pointer; font-size: 14px; }
                    .tip { color: #6b7280; font-size: 13px; margin-top: 12px; }
                  </style>
                </head>
                <body>
                  <div class="card">
                    <div class="badge">Mock IDaaS</div>
                    <h1>授权确认</h1>
                    <p>当前登录用户 <code>%s</code>（%s） 正在为业务 Agent 授权以下权限点，用于后续换取可访问资源的 <code>TR</code>。</p>
                    <div class="consent-box">
                      <strong>本次申请范围：</strong>
                      <ul>%s</ul>
                    </div>
                    <form method="post" action="/mock/idaas/approve">
                      <input type="hidden" name="flow" value="consent"/>
                      <input type="hidden" name="redirect_uri" value="%s"/>
                      <input type="hidden" name="scope" value="%s"/>
                      <input type="hidden" name="state" value="%s"/>
                      <input type="hidden" name="user_id" value="%s"/>
                      <input type="hidden" name="username" value="%s"/>
                      <div class="checkbox">
                        <input id="approved" name="approved" type="checkbox" value="true" required/>
                        <label for="approved">我已阅读并同意将以上权限授权给当前业务 Agent，用于本次资源访问。</label>
                      </div>
                      <button type="submit">确认授权并继续</button>
                      <div class="tip">这里会沿用当前已登录用户，不会回退成默认演示账号。</div>
                    </form>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(userId),
                escapeHtml(username),
                scopeItems,
                escapeHtml(redirectUri),
                escapeHtml(scope),
                escapeHtml(state),
                escapeHtml(userId),
                escapeHtml(username)
        );
    }

    private boolean isApproved(String approved) {
        return "true".equalsIgnoreCase(approved) || "on".equalsIgnoreCase(approved);
    }

    private String trimToDefault(String value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? defaultValue : trimmed;
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String encodeMockUser(String userId, String username) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString((userId + "\n" + username).getBytes(StandardCharsets.UTF_8));
    }

    private MockUser parseMockUser(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\n", 2);
            if (parts.length != 2 || parts[0].isBlank()) {
                return null;
            }
            return new MockUser(parts[0], parts[1].isBlank() ? parts[0] : parts[1]);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private record MockUser(String userId, String username) {
    }
}

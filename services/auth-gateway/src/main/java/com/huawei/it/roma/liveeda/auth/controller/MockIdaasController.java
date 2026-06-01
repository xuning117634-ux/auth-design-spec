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

    @GetMapping(value = "/authorize", produces = "text/html;charset=UTF-8")
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
                : parseScope(scope);
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
        return commonPageHead("Mock IDaaS 登录")
                + """
                <body>
                  <main class="page-shell">
                    <section class="identity-panel">
                      <div class="brand-row">
                        <div class="brand-mark"><span class="material-symbols-outlined">shield_person</span></div>
                        <div>
                          <div class="brand-name">Mock IDaaS</div>
                          <div class="brand-subtitle">统一身份认证中心</div>
                        </div>
                      </div>
                      <div class="hero-copy">
                        <div class="eyebrow">BASE LOGIN</div>
                        <p>这里模拟 IDaaS 登录页面。浏览器回到 Agent 网关时只携带授权码和 state，用户信息会在后端通过 ticketST 交换获取。</p>
                      </div>
                      <div class="timeline">
                        <div class="step"><span class="dot active"></span><span>输入账号并提交登录</span></div>
                        <div class="step"><span class="dot"></span><span>网关生成一次性 ticketST</span></div>
                        <div class="step"><span class="dot"></span><span>业务 Agent 后端换取用户信息</span></div>
                      </div>
                    </section>

                    <section class="card">
                      <div class="card-head">
                        <div>
                          <div class="badge">Mock Profile</div>
                          <h2>登录到统一身份认证平台</h2>
                        </div>
                        <span class="material-symbols-outlined head-icon">login</span>
                      </div>
                      <form method="post" action="/mock/idaas/approve">
                        <input type="hidden" name="flow" value="base"/>
                        <input type="hidden" name="redirect_uri" value="%s"/>
                        <input type="hidden" name="scope" value="%s"/>
                        <input type="hidden" name="state" value="%s"/>

                        <label for="user_id">登录账号</label>
                        <input id="user_id" name="user_id" type="text" value="%s" autocomplete="username"/>

                        <label for="username">显示名称</label>
                        <input id="username" name="username" type="text" value="%s"/>

                        <label for="password">密码</label>
                        <input id="password" name="password" type="password" value="MockPassword@123" autocomplete="current-password"/>

                        <div class="meta-grid">
                          <div class="meta-item"><span>流程</span><code>base</code></div>
                          <div class="meta-item"><span>scope</span><code>%s</code></div>
                          <div class="meta-item"><span>state</span><code>%s</code></div>
                        </div>

                        <button class="primary-button" type="submit">
                          <span class="material-symbols-outlined">arrow_forward</span>
                          登录并继续
                        </button>
                      </form>
                      <p class="tip">演示环境会默认填充账号、姓名和密码；真实环境中登录态由 IDaaS 自己维护。</p>
                    </section>
                  </main>
                </body>
                </html>
                """.formatted(
                        escapeHtml(redirectUri),
                        escapeHtml(scope),
                        escapeHtml(state),
                        escapeHtml(properties.getDefaultUserId()),
                        escapeHtml(properties.getDefaultUsername()),
                        escapeHtml(scope),
                        escapeHtml(state)
                );
    }

    private String renderConsentPage(String redirectUri, String scope, String state, String mockIdaasSession) {
        Set<String> permissionPointCodes = parseScope(scope);
        String scopeItems = policyCenterClient.resolveByCodes(permissionPointCodes).stream()
                .sorted(Comparator.comparing(AuthorizedPermissionPoint::code))
                .map(point -> """
                        <li class="scope-item">
                          <div>
                            <code>%s</code>
                            <p>%s</p>
                          </div>
                          <span class="material-symbols-outlined">verified_user</span>
                        </li>
                        """.formatted(escapeHtml(point.code()), escapeHtml(point.displayNameZh())))
                .collect(Collectors.joining());

        MockUser mockUser = parseMockUser(mockIdaasSession);
        String userId = mockUser == null ? properties.getDefaultUserId() : mockUser.userId();
        String username = mockUser == null ? properties.getDefaultUsername() : mockUser.username();

        return commonPageHead("Mock IDaaS 授权确认")
                + """
                <body>
                  <main class="page-shell">
                    <section class="identity-panel">
                      <div class="brand-row">
                        <div class="brand-mark"><span class="material-symbols-outlined">key</span></div>
                        <div>
                          <div class="brand-name">Mock IDaaS</div>
                          <div class="brand-subtitle">权限授权确认</div>
                        </div>
                      </div>
                      <div class="hero-copy">
                        <div class="eyebrow">CONSENT</div>
                        <h2>确认是否把这些权限点授权给业务 Agent?</h2>
                        <p>授权完成后，Agent 网关会用 code 换取 Tc，再结合业务 Agent 的 T1 换取 TR。浏览器 URL 不会出现 Tc 或 TR。</p>
                      </div>
                      <div class="user-card">
                        <span class="material-symbols-outlined">account_circle</span>
                        <div>
                          <strong>%s</strong>
                          <code>%s</code>
                        </div>
                      </div>
                    </section>

                    <section class="card">
                      <div class="card-head">
                        <div>
                          <div class="badge warning">Permission Points</div>
                          <h2>本次申请的权限范围</h2>
                        </div>
                        <span class="material-symbols-outlined head-icon">rule</span>
                      </div>

                      <div class="scope-box">
                        <ul>%s</ul>
                      </div>

                      <form method="post" action="/mock/idaas/approve">
                        <input type="hidden" name="flow" value="consent"/>
                        <input type="hidden" name="redirect_uri" value="%s"/>
                        <input type="hidden" name="scope" value="%s"/>
                        <input type="hidden" name="state" value="%s"/>
                        <input type="hidden" name="user_id" value="%s"/>
                        <input type="hidden" name="username" value="%s"/>

                        <label class="confirm-row" for="approved">
                          <input id="approved" name="approved" type="checkbox" value="true" required/>
                          <span>我已确认以上权限点，并同意将其授权给当前业务 Agent 用于本次资源访问。</span>
                        </label>

                        <div class="meta-grid">
                          <div class="meta-item"><span>授权对象</span><code>TR consent</code></div>
                          <div class="meta-item"><span>state</span><code>%s</code></div>
                        </div>

                        <button class="primary-button" type="submit">
                          <span class="material-symbols-outlined">task_alt</span>
                          确认授权并继续
                        </button>
                      </form>
                      <p class="tip">这里会沿用当前 Mock IDaaS 登录用户，不会把默认演示账号静默替换回去。</p>
                    </section>
                  </main>
                </body>
                </html>
                """.formatted(
                        escapeHtml(username),
                        escapeHtml(userId),
                        scopeItems,
                        escapeHtml(redirectUri),
                        escapeHtml(scope),
                        escapeHtml(state),
                        escapeHtml(userId),
                        escapeHtml(username),
                        escapeHtml(state)
                );
    }

    private String commonPageHead(String title) {
        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>""" + escapeHtml(title) + """
                  </title>
                  <link rel="preconnect" href="https://fonts.googleapis.com">
                  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
                  <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&display=swap" rel="stylesheet">
                  <style>
                    :root {
                      --bg: #0d0e15;
                      --surface: #12131a;
                      --panel: #1a1b22;
                      --panel-high: #292931;
                      --line: #33343c;
                      --line-soft: #464554;
                      --text: #e3e1ec;
                      --muted: #9ca0b5;
                      --muted-strong: #c7c4d7;
                      --primary: #8083ff;
                      --tertiary: #ffb783;
                      --success: #52e6a2;
                    }
                    * { box-sizing: border-box; }
                    body {
                      margin: 0;
                      min-height: 100vh;
                      color: var(--text);
                      font-family: Inter, "Microsoft YaHei", "PingFang SC", sans-serif;
                      background:
                        radial-gradient(circle at 16% -8%, rgba(128, 131, 255, 0.22), transparent 34rem),
                        radial-gradient(circle at 90% 8%, rgba(255, 183, 131, 0.10), transparent 28rem),
                        var(--bg);
                    }
                    .material-symbols-outlined {
                      font-variation-settings: 'FILL' 0, 'wght' 500, 'GRAD' 0, 'opsz' 24;
                      line-height: 1;
                    }
                    .page-shell {
                      width: min(1120px, calc(100vw - 40px));
                      min-height: 100vh;
                      margin: 0 auto;
                      display: grid;
                      grid-template-columns: 0.95fr 1.05fr;
                      gap: 28px;
                      align-items: center;
                      padding: 42px 0;
                    }
                    .identity-panel, .card {
                      border: 1px solid rgba(70, 69, 84, 0.72);
                      border-radius: 22px;
                      background: rgba(18, 19, 26, 0.78);
                      box-shadow: 0 24px 80px rgba(0, 0, 0, 0.32);
                      backdrop-filter: blur(18px);
                    }
                    .identity-panel {
                      min-height: 560px;
                      padding: 28px;
                      display: flex;
                      flex-direction: column;
                      justify-content: space-between;
                      background:
                        linear-gradient(150deg, rgba(128, 131, 255, 0.16), rgba(255, 183, 131, 0.07)),
                        rgba(13, 14, 21, 0.82);
                    }
                    .brand-row { display: flex; align-items: center; gap: 14px; }
                    .brand-mark {
                      width: 48px;
                      height: 48px;
                      border-radius: 16px;
                      display: grid;
                      place-items: center;
                      color: var(--primary);
                      background: rgba(128, 131, 255, 0.16);
                      border: 1px solid rgba(128, 131, 255, 0.36);
                      box-shadow: 0 0 34px rgba(128, 131, 255, 0.20);
                    }
                    .brand-name { font-size: 24px; font-weight: 700; letter-spacing: -0.04em; }
                    .brand-subtitle { color: var(--muted); font-size: 13px; margin-top: 3px; }
                    .eyebrow {
                      color: var(--tertiary);
                      font-family: "JetBrains Mono", monospace;
                      font-size: 12px;
                      letter-spacing: 0.10em;
                      margin-bottom: 12px;
                    }
                    h1, h2, p { margin: 0; }
                    h1 { font-size: clamp(32px, 5vw, 52px); line-height: 1.05; letter-spacing: -0.055em; }
                    h2 { font-size: 25px; letter-spacing: -0.035em; }
                    .hero-copy p, .tip {
                      color: var(--muted-strong);
                      line-height: 1.75;
                      margin-top: 16px;
                    }
                    .timeline { display: grid; gap: 12px; color: var(--muted-strong); }
                    .step { display: flex; align-items: center; gap: 10px; font-size: 14px; }
                    .dot {
                      width: 8px;
                      height: 8px;
                      border-radius: 999px;
                      background: var(--line-soft);
                    }
                    .dot.active { background: var(--success); box-shadow: 0 0 14px rgba(82, 230, 162, 0.75); }
                    .card { padding: 26px; }
                    .card-head {
                      display: flex;
                      justify-content: space-between;
                      gap: 16px;
                      align-items: flex-start;
                      padding-bottom: 20px;
                      border-bottom: 1px solid rgba(70, 69, 84, 0.62);
                      margin-bottom: 22px;
                    }
                    .badge {
                      display: inline-flex;
                      margin-bottom: 10px;
                      border: 1px solid rgba(128, 131, 255, 0.34);
                      color: #dfe0ff;
                      background: rgba(128, 131, 255, 0.13);
                      border-radius: 999px;
                      padding: 6px 10px;
                      font-family: "JetBrains Mono", monospace;
                      font-size: 12px;
                    }
                    .badge.warning {
                      border-color: rgba(255, 183, 131, 0.34);
                      color: #ffd9bf;
                      background: rgba(255, 183, 131, 0.10);
                    }
                    .head-icon {
                      color: var(--primary);
                      border: 1px solid rgba(128, 131, 255, 0.28);
                      background: rgba(128, 131, 255, 0.12);
                      border-radius: 14px;
                      padding: 10px;
                    }
                    label {
                      display: block;
                      color: var(--muted-strong);
                      font-weight: 600;
                      font-size: 13px;
                      margin: 16px 0 8px;
                    }
                    input[type=text], input[type=password] {
                      width: 100%;
                      height: 46px;
                      border: 1px solid rgba(70, 69, 84, 0.82);
                      border-radius: 12px;
                      background: rgba(13, 14, 21, 0.86);
                      color: var(--text);
                      padding: 0 14px;
                      outline: none;
                      font: inherit;
                    }
                    input:focus {
                      border-color: rgba(128, 131, 255, 0.72);
                      box-shadow: 0 0 0 3px rgba(128, 131, 255, 0.15);
                    }
                    .meta-grid {
                      display: grid;
                      grid-template-columns: repeat(2, minmax(0, 1fr));
                      gap: 10px;
                      margin-top: 18px;
                    }
                    .meta-item, .user-card {
                      border: 1px solid rgba(70, 69, 84, 0.70);
                      background: rgba(26, 27, 34, 0.74);
                      border-radius: 12px;
                      padding: 11px 12px;
                    }
                    .meta-item span {
                      display: block;
                      color: var(--muted);
                      font-size: 12px;
                      margin-bottom: 6px;
                    }
                    code {
                      color: #e6e6ff;
                      font-family: "JetBrains Mono", monospace;
                      font-size: 12px;
                      word-break: break-all;
                    }
                    .primary-button {
                      width: 100%;
                      height: 48px;
                      border: none;
                      border-radius: 13px;
                      margin-top: 20px;
                      cursor: pointer;
                      display: inline-flex;
                      align-items: center;
                      justify-content: center;
                      gap: 8px;
                      color: white;
                      font-weight: 700;
                      background: linear-gradient(135deg, #8083ff, #6366f1);
                      box-shadow: 0 12px 32px rgba(99, 102, 241, 0.34);
                    }
                    .primary-button:hover { filter: brightness(1.08); transform: translateY(-1px); }
                    .scope-box {
                      border: 1px solid rgba(70, 69, 84, 0.72);
                      border-radius: 16px;
                      background: rgba(13, 14, 21, 0.72);
                      padding: 8px;
                    }
                    .scope-box ul { list-style: none; margin: 0; padding: 0; display: grid; gap: 8px; }
                    .scope-item {
                      display: flex;
                      justify-content: space-between;
                      align-items: center;
                      gap: 14px;
                      border: 1px solid rgba(70, 69, 84, 0.58);
                      border-radius: 12px;
                      padding: 12px;
                      background: rgba(26, 27, 34, 0.72);
                    }
                    .scope-item p { color: var(--muted-strong); margin-top: 6px; line-height: 1.5; }
                    .scope-item .material-symbols-outlined { color: var(--success); }
                    .confirm-row {
                      display: flex;
                      align-items: flex-start;
                      gap: 12px;
                      border: 1px solid rgba(255, 183, 131, 0.28);
                      background: rgba(255, 183, 131, 0.08);
                      border-radius: 14px;
                      padding: 14px;
                      line-height: 1.65;
                    }
                    .confirm-row input { margin-top: 5px; accent-color: #8083ff; }
                    .user-card { display: flex; align-items: center; gap: 12px; }
                    .user-card .material-symbols-outlined { color: var(--primary); }
                    .user-card strong { display: block; margin-bottom: 4px; }
                    @media (max-width: 860px) {
                      .page-shell { grid-template-columns: 1fr; padding: 20px 0; }
                      .identity-panel { min-height: auto; gap: 56px; }
                      .meta-grid { grid-template-columns: 1fr; }
                    }
                  </style>
                </head>
                """;
    }

    private boolean isApproved(String approved) {
        return "true".equalsIgnoreCase(approved) || "on".equalsIgnoreCase(approved);
    }

    private Set<String> parseScope(String scope) {
        if (scope == null || scope.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(scope.trim().split("\\s+"))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toSet());
    }

    private String trimToDefault(String value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? defaultValue : trimmed;
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
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

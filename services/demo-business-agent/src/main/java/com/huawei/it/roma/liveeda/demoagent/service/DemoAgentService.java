package com.huawei.it.roma.liveeda.demoagent.service;

import com.huawei.it.roma.liveeda.demoagent.client.AgentGatewayClient;
import com.huawei.it.roma.liveeda.demoagent.client.GatewayTokenResponse;
import com.huawei.it.roma.liveeda.demoagent.client.LoginTicketExchangeResponse;
import com.huawei.it.roma.liveeda.demoagent.config.DemoAgentProperties;
import com.huawei.it.roma.liveeda.demoagent.domain.SiteSession;
import com.huawei.it.roma.liveeda.demoagent.domain.TrCacheEntry;
import com.huawei.it.roma.liveeda.demoagent.store.SiteSessionStore;
import com.huawei.it.roma.liveeda.demoagent.store.TrCacheStore;
import com.huawei.it.roma.liveeda.demoagent.util.IdGenerator;
import com.huawei.it.roma.liveeda.demoagent.web.ChatResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class DemoAgentService {

    public static final String SITE_SESSION_COOKIE = "site_session_id";

    private final DemoAgentProperties properties;
    private final SiteSessionStore siteSessionStore;
    private final TrCacheStore trCacheStore;
    private final AgentGatewayClient agentGatewayClient;
    private final McpGatewayClient mcpGatewayClient;
    private final IdGenerator idGenerator;
    private final Clock clock = Clock.systemUTC();

    public SiteSession createSiteSession(String userId, String username) {
        SiteSession siteSession = new SiteSession(
                idGenerator.next("site"),
                userId,
                username,
                clock.instant()
        );
        siteSessionStore.save(siteSession);
        return siteSession;
    }

    public SiteSession createSiteSessionFromTicket(String ticketST) {
        LoginTicketExchangeResponse response = agentGatewayClient.exchangeLoginTicket(properties.getAgentId(), ticketST);
        if (response == null || response.user() == null || response.user().userId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Agent 网关未返回可用的登录用户信息");
        }
        return createSiteSession(response.user().userId(), response.user().username());
    }

    public void exchangeTokenResult(String siteSessionId, String requestId, String tokenResultTicket) {
        SiteSession siteSession = siteSessionStore.find(siteSessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "当前站点会话不存在，请重新登录"));
        GatewayTokenResponse response = agentGatewayClient.exchangeTokenResult(
                properties.getAgentId(),
                requestId,
                tokenResultTicket
        );
        if (response == null || !response.isTokenReady()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Agent 网关未返回可用的资源令牌");
        }
        ensureSameUser(siteSession, response);
        saveTrCache(siteSession, response);
    }

    public void logout(String siteSessionId) {
        if (siteSessionId == null || siteSessionId.isBlank()) {
            return;
        }
        trCacheStore.delete(siteSessionId, properties.getAgentId());
        siteSessionStore.delete(siteSessionId);
    }

    public ChatResponse handleChat(String siteSessionId, String message) {
        SiteSession siteSession = siteSessionStore.find(siteSessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "当前站点会话不存在，请重新进入 Agent 页面"));

        Set<String> requiredTools = mapRequiredTools(message);
        if (requiredTools.isEmpty()) {
            return ChatResponse.answer("""
                    这个问题当前不需要调用外部工具，我先给你一个本地回答。
                    如果你后面问到财报、合同或发票之类的内容，我会再触发网关授权并调用对应能力。
                    原始问题：%s
                    """.formatted(message).trim(), "local");
        }

        Optional<TrCacheEntry> trCacheEntry = trCacheStore.find(siteSession.siteSessionId(), properties.getAgentId());
        if (trCacheEntry.isPresent() && trCacheEntry.get().covers(requiredTools, clock.instant())) {
            String answer = mcpGatewayClient.invoke(
                    properties.getAgentId(),
                    trCacheEntry.get().currentTr(),
                    requiredTools,
                    message
            );
            return ChatResponse.answer("""
                    已复用当前会话下已有的授权结果，无需再次跳转授权。
                    %s
                    """.formatted(answer).trim(), "cache");
        }

        String outerState = idGenerator.next("st_auth");
        GatewayTokenResponse gatewayTokenResponse = agentGatewayClient.requestResourceToken(
                properties.getAgentId(),
                requiredTools.stream().sorted().toList(),
                properties.getSelfBaseUrl() + "/agent",
                outerState
        );
        if (gatewayTokenResponse.isRedirect()) {
            return ChatResponse.redirect(gatewayTokenResponse.redirectUrl(), outerState);
        }
        if (!gatewayTokenResponse.isTokenReady()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Agent 网关未返回可用的资源令牌");
        }

        saveTrCache(siteSession, gatewayTokenResponse);
        String answer = mcpGatewayClient.invoke(
                properties.getAgentId(),
                gatewayTokenResponse.accessToken(),
                requiredTools,
                message
        );
        return ChatResponse.answer("""
                本次已完成授权并成功获取资源令牌，下面是模拟 Agent 的回答。
                %s
                """.formatted(answer).trim(), "gateway");
    }

    private void ensureSameUser(SiteSession siteSession, GatewayTokenResponse response) {
        if (response.agencyUser() == null || response.agencyUser().userId() == null) {
            return;
        }
        String tokenUserId = response.agencyUser().userId();
        String tokenGlobalUserId = response.agencyUser().globalUserId();
        boolean matched = siteSession.userId().equals(tokenUserId) || siteSession.userId().equals(tokenGlobalUserId);
        if (!matched) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "TR 所属用户与当前站点会话不一致，请重新登录");
        }
    }

    private void saveTrCache(SiteSession siteSession, GatewayTokenResponse gatewayTokenResponse) {
        Set<String> coveredPermissionPointCodes = gatewayTokenResponse.consentedScopes() == null
                ? Set.of()
                : gatewayTokenResponse.consentedScopes().stream()
                        .map(GatewayTokenResponse.ConsentedScope::code)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        if (coveredPermissionPointCodes.isEmpty()) {
            coveredPermissionPointCodes = mcpGatewayClient.extractAuthorizedPermissionPointCodes(
                    gatewayTokenResponse.accessToken());
        }
        TrCacheEntry merged = new TrCacheEntry(
                siteSession.siteSessionId(),
                properties.getAgentId(),
                gatewayTokenResponse.accessToken(),
                mcpGatewayClient.resolveCoveredTools(gatewayTokenResponse.accessToken()),
                coveredPermissionPointCodes,
                Instant.now(clock).plusSeconds(gatewayTokenResponse.expiresIn() == null
                        ? 3600
                        : gatewayTokenResponse.expiresIn())
        );
        trCacheStore.save(merged);
    }

    private Set<String> mapRequiredTools(String message) {
        String normalized = message == null ? "" : message.toLowerCase();
        Set<String> tools = new LinkedHashSet<>();
        if (normalized.contains("财报") || normalized.contains("report")) {
            tools.add("mcp:financial-report-server/query_monthly_report");
        }
        if (normalized.contains("合同") || normalized.contains("contract")) {
            tools.add("mcp:contract-server/get_contract");
        }
        if (normalized.contains("发票") || normalized.contains("invoice")) {
            tools.add("mcp:invoice-server/query_invoices");
        }
        return tools;
    }
}

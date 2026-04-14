package com.huawei.it.roma.liveeda.demoagent.service;

import com.huawei.it.roma.liveeda.demoagent.client.AgentGatewayClient;
import com.huawei.it.roma.liveeda.demoagent.client.GatewayTokenResponse;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
    private final MockMcpClient mockMcpClient;
    private final IdGenerator idGenerator;
    private final Clock clock = Clock.systemUTC();

    public SiteSession createSiteSession(String gwSessionToken, String userId, String username) {
        SiteSession siteSession = new SiteSession(
                idGenerator.next("site"),
                gwSessionToken,
                userId,
                username,
                clock.instant()
        );
        siteSessionStore.save(siteSession);
        return siteSession;
    }

    public ChatResponse handleChat(String siteSessionId, String message) {
        SiteSession siteSession = siteSessionStore.find(siteSessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing site session"));

        Set<String> requiredTools = mapRequiredTools(message);
        if (requiredTools.isEmpty()) {
            return ChatResponse.answer("""
                    这个问题当前不需要调用外部工具，我先直接给你一个本地回答。

                    如果后面你问到财报、发票之类的内容，我会再触发网关授权并调用对应能力。

                    原始问题：%s
                    """.formatted(message).trim(), "local");
        }

        Optional<TrCacheEntry> trCacheEntry = trCacheStore.find(siteSession.siteSessionId(), properties.getAgentId());
        if (trCacheEntry.isPresent() && trCacheEntry.get().covers(requiredTools, clock.instant())) {
            String answer = mockMcpClient.invoke(message, requiredTools, trCacheEntry.get().currentTr());
            return ChatResponse.answer("""
                    已复用当前会话下已有的授权结果，无需再次跳转授权。

                    %s
                    """.formatted(answer).trim(), "cache");
        }

        String outerState = idGenerator.next("st_auth");
        GatewayTokenResponse gatewayTokenResponse = agentGatewayClient.requestResourceToken(
                siteSession.gwSessionToken(),
                properties.getAgentId(),
                requiredTools.stream().sorted().toList(),
                properties.getSelfBaseUrl() + "/agent.html",
                outerState
        );
        if (gatewayTokenResponse.isRedirect()) {
            return ChatResponse.redirect(gatewayTokenResponse.redirectUrl(), outerState);
        }
        if (!gatewayTokenResponse.isTokenReady()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Agent gateway did not return a TR");
        }

        TrCacheEntry merged = new TrCacheEntry(
                siteSession.siteSessionId(),
                properties.getAgentId(),
                gatewayTokenResponse.accessToken(),
                mergeCoveredTools(trCacheEntry.map(TrCacheEntry::coveredTools).orElse(Set.of()), requiredTools),
                Set.of(),
                Instant.now(clock).plusSeconds(gatewayTokenResponse.expiresIn() == null ? 3600 : gatewayTokenResponse.expiresIn())
        );
        trCacheStore.save(merged);

        String answer = mockMcpClient.invoke(message, requiredTools, gatewayTokenResponse.accessToken());
        return ChatResponse.answer("""
                本次已完成授权并成功获取资源令牌，下面是模拟 Agent 的回答。

                %s
                """.formatted(answer).trim(), "gateway");
    }

    private Set<String> mapRequiredTools(String message) {
        String normalized = message == null ? "" : message.toLowerCase();
        Set<String> tools = new LinkedHashSet<>();
        if (normalized.contains("\u8d22\u62a5") || normalized.contains("report")) {
            tools.add("mcp:financial-report-server/query_monthly_report");
            tools.add("mcp:financial-report-server/list_report_categories");
        }
        if (normalized.contains("\u53d1\u7968") || normalized.contains("invoice")) {
            tools.add("mcp:invoice-server/query_invoices");
        }
        return tools;
    }

    private Set<String> mergeCoveredTools(Set<String> currentTools, Set<String> newTools) {
        Set<String> merged = new LinkedHashSet<>(currentTools);
        merged.addAll(newTools);
        return merged;
    }
}

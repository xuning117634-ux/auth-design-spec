package com.huawei.it.roma.liveeda.auth.store;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.huawei.it.roma.liveeda.auth.domain.GatewaySession;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class GatewaySessionStore {

    private final Cache<String, GatewaySession> sessionsById = Caffeine.newBuilder()
            .expireAfterWrite(8, TimeUnit.HOURS)
            .maximumSize(10_000)
            .build();

    private final Cache<String, String> sessionIdByToken = Caffeine.newBuilder()
            .expireAfterWrite(8, TimeUnit.HOURS)
            .maximumSize(10_000)
            .build();

    public void save(GatewaySession session) {
        sessionsById.put(session.gatewaySessionId(), session);
        sessionIdByToken.put(session.gatewaySessionToken(), session.gatewaySessionId());
    }

    public Optional<GatewaySession> findByToken(String token) {
        String sessionId = sessionIdByToken.getIfPresent(token);
        return sessionId == null ? Optional.empty() : findById(sessionId);
    }

    public Optional<GatewaySession> findById(String sessionId) {
        return Optional.ofNullable(sessionsById.getIfPresent(sessionId));
    }
}

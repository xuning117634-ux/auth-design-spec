package com.huawei.it.roma.liveeda.auth.store;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.huawei.it.roma.liveeda.auth.domain.GatewayAuthContext;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class GatewayAuthContextStore {

    private final Cache<String, GatewayAuthContext> cache = Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.HOURS)
            .maximumSize(10_000)
            .build();

    public void save(GatewayAuthContext context) {
        cache.put(key(context.gatewaySessionId(), context.agentId()), context);
    }

    public Optional<GatewayAuthContext> find(String gatewaySessionId, String agentId) {
        return Optional.ofNullable(cache.getIfPresent(key(gatewaySessionId, agentId)));
    }

    private String key(String gatewaySessionId, String agentId) {
        return gatewaySessionId + "::" + agentId;
    }
}

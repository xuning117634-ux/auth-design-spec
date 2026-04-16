package com.huawei.it.roma.liveeda.demoagent.store;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.huawei.it.roma.liveeda.demoagent.domain.TrCacheEntry;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class TrCacheStore {

    private final Cache<String, TrCacheEntry> cache = Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.HOURS)
            .maximumSize(10_000)
            .build();

    public void save(TrCacheEntry entry) {
        cache.put(key(entry.siteSessionId(), entry.agentId()), entry);
    }

    public Optional<TrCacheEntry> find(String siteSessionId, String agentId) {
        return Optional.ofNullable(cache.getIfPresent(key(siteSessionId, agentId)));
    }

    public void delete(String siteSessionId, String agentId) {
        cache.invalidate(key(siteSessionId, agentId));
    }

    private String key(String siteSessionId, String agentId) {
        return siteSessionId + "::" + agentId;
    }
}

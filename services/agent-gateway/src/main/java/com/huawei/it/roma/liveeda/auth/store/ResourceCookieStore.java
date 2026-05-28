package com.huawei.it.roma.liveeda.auth.store;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.huawei.it.roma.liveeda.auth.domain.ResourceCookieEntry;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ResourceCookieStore {

    private final Cache<String, ResourceCookieEntry> cache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(24))
            .maximumSize(100_000)
            .build();

    public void save(ResourceCookieEntry entry) {
        cache.put(cacheKey(entry.agentId(), entry.trHash()), entry);
    }

    public Optional<ResourceCookieEntry> find(String agentId, String trHash, Instant now) {
        String key = cacheKey(agentId, trHash);
        ResourceCookieEntry entry = cache.getIfPresent(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (!entry.expiresAt().isAfter(now)) {
            cache.invalidate(key);
            return Optional.empty();
        }
        return Optional.of(entry);
    }

    private String cacheKey(String agentId, String trHash) {
        return agentId + ":" + trHash;
    }
}

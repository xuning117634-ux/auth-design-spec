package com.huawei.it.roma.liveeda.demoagent.store;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.huawei.it.roma.liveeda.demoagent.domain.SiteSession;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class SiteSessionStore {

    private final Cache<String, SiteSession> cache = Caffeine.newBuilder()
            .expireAfterWrite(8, TimeUnit.HOURS)
            .maximumSize(10_000)
            .build();

    public void save(SiteSession siteSession) {
        cache.put(siteSession.siteSessionId(), siteSession);
    }

    public Optional<SiteSession> find(String siteSessionId) {
        return Optional.ofNullable(cache.getIfPresent(siteSessionId));
    }
}

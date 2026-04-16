package com.huawei.it.roma.liveeda.auth.store;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("mock")
public class MockIdaasGrantStore {

    private final Cache<String, MockIdaasGrant> cache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    public void save(String code, MockIdaasGrant grant) {
        cache.put(code, grant);
    }

    public Optional<MockIdaasGrant> consume(String code) {
        MockIdaasGrant grant = cache.getIfPresent(code);
        cache.invalidate(code);
        return Optional.ofNullable(grant);
    }

    public record MockIdaasGrant(
            String flow,
            String userId,
            String username,
            Set<String> permissionPointCodes
    ) {
    }
}

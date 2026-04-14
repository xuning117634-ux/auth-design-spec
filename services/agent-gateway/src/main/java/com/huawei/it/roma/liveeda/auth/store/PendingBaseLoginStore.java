package com.huawei.it.roma.liveeda.auth.store;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.huawei.it.roma.liveeda.auth.domain.PendingBaseLogin;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class PendingBaseLoginStore {

    private final Cache<String, PendingBaseLogin> cache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    public void save(PendingBaseLogin pendingBaseLogin) {
        cache.put(pendingBaseLogin.gwState(), pendingBaseLogin);
    }

    public Optional<PendingBaseLogin> find(String gwState) {
        return Optional.ofNullable(cache.getIfPresent(gwState));
    }

    public void delete(String gwState) {
        cache.invalidate(gwState);
    }
}

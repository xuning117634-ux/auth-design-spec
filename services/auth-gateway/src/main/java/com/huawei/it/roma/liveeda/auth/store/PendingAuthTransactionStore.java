package com.huawei.it.roma.liveeda.auth.store;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.huawei.it.roma.liveeda.auth.domain.PendingAuthTransaction;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class PendingAuthTransactionStore {

    private final Cache<String, PendingAuthTransaction> byRequestId = Caffeine.newBuilder()
            .expireAfterWrite(20, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    private final Cache<String, String> requestIdByGwState = Caffeine.newBuilder()
            .expireAfterWrite(20, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    public void save(PendingAuthTransaction transaction) {
        byRequestId.put(transaction.requestId(), transaction);
        if (transaction.gwState() != null) {
            requestIdByGwState.put(transaction.gwState(), transaction.requestId());
        }
    }

    public Optional<PendingAuthTransaction> findByRequestId(String requestId) {
        return Optional.ofNullable(byRequestId.getIfPresent(requestId));
    }

    public Optional<PendingAuthTransaction> findByGwState(String gwState) {
        String requestId = requestIdByGwState.getIfPresent(gwState);
        return requestId == null ? Optional.empty() : findByRequestId(requestId);
    }

    public void delete(PendingAuthTransaction transaction) {
        byRequestId.invalidate(transaction.requestId());
        if (transaction.gwState() != null) {
            requestIdByGwState.invalidate(transaction.gwState());
        }
    }
}

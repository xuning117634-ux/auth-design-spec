package com.huawei.it.roma.liveeda.auth.store;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.huawei.it.roma.liveeda.auth.domain.TokenResultTicket;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class TokenResultTicketStore {

    private final Cache<String, TokenResultTicket> cache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    public void save(TokenResultTicket ticket) {
        cache.put(ticket.tokenResultTicket(), ticket);
    }

    public Optional<TokenResultTicket> find(String tokenResultTicket) {
        return Optional.ofNullable(cache.getIfPresent(tokenResultTicket));
    }

    public void delete(String tokenResultTicket) {
        cache.invalidate(tokenResultTicket);
    }
}

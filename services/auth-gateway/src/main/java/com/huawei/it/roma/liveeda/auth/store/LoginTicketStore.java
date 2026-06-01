package com.huawei.it.roma.liveeda.auth.store;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.huawei.it.roma.liveeda.auth.domain.LoginTicket;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class LoginTicketStore {

    private final Cache<String, LoginTicket> cache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    public void save(LoginTicket ticket) {
        cache.put(ticket.ticketST(), ticket);
    }

    public Optional<LoginTicket> find(String ticketST) {
        return Optional.ofNullable(cache.getIfPresent(ticketST));
    }

    public void delete(String ticketST) {
        cache.invalidate(ticketST);
    }
}

package com.huawei.it.roma.liveeda.auth.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.huawei.it.roma.liveeda.auth.domain.ResourceCookieEntry;
import com.huawei.it.roma.liveeda.auth.store.ResourceCookieStore;
import com.huawei.it.roma.liveeda.auth.util.LogSanitizer;
import com.huawei.it.roma.liveeda.auth.web.GatewayException;
import com.huawei.it.roma.liveeda.auth.web.TrCookieResolveRequest;
import com.huawei.it.roma.liveeda.auth.web.TrCookieResolveResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceCookieService {

    private final ResourceCookieStore resourceCookieStore;
    private final Clock clock;

    public void cacheCookie(String agentId, String trToken, Instant trExpiresAt, String cookieHeader) {
        if (cookieHeader == null || cookieHeader.isBlank()) {
            return;
        }
        String tokenAudience = extractAudience(trToken);
        if (!agentId.equals(tokenAudience)) {
            log.warn("resource cookie cache rejected, reason=aud_mismatch, agentId={}, tokenAudience={}",
                    agentId, tokenAudience);
            throw new GatewayException(HttpStatus.UNAUTHORIZED, "TR aud does not match current agent");
        }
        Instant now = clock.instant();
        if (!trExpiresAt.isAfter(now)) {
            log.info("resource cookie cache skipped, reason=tr_expired, agentId={}, trExpiresAt={}",
                    agentId, trExpiresAt);
            return;
        }
        resourceCookieStore.save(new ResourceCookieEntry(
                agentId,
                sha256(trToken),
                cookieHeader,
                trExpiresAt,
                now
        ));
        log.info("resource cookie cached, agentId={}, trTail={}, expiresAt={}",
                agentId, LogSanitizer.tail(trToken), trExpiresAt);
    }

    public TrCookieResolveResponse resolve(TrCookieResolveRequest request) {
        String tokenAudience = extractAudience(request.tr());
        if (!request.agentId().equals(tokenAudience)) {
            log.warn("resource cookie resolve rejected, reason=aud_mismatch, agentId={}, tokenAudience={}",
                    request.agentId(), tokenAudience);
            throw new GatewayException(HttpStatus.UNAUTHORIZED, "TR aud does not match current agent");
        }
        TrCookieResolveResponse response = resourceCookieStore.find(request.agentId(), sha256(request.tr()), clock.instant())
                .map(entry -> new TrCookieResolveResponse(
                        true,
                        entry.cookie(),
                        Math.max(0, entry.expiresAt().getEpochSecond() - clock.instant().getEpochSecond())
                ))
                .orElseGet(() -> new TrCookieResolveResponse(false, null, null));
        log.info("resource cookie resolve completed, agentId={}, found={}, expiresIn={}",
                request.agentId(), response.found(), response.expiresIn());
        return response;
    }

    private String extractAudience(String trToken) {
        try {
            DecodedJWT decodedJWT = JWT.decode(trToken);
            List<String> audience = decodedJWT.getAudience();
            if (audience != null && !audience.isEmpty() && audience.getFirst() != null
                    && !audience.getFirst().isBlank()) {
                return audience.getFirst();
            }
            String claimAudience = decodedJWT.getClaim("aud").asString();
            if (claimAudience != null && !claimAudience.isBlank()) {
                return claimAudience;
            }
            throw new GatewayException(HttpStatus.UNAUTHORIZED, "TR missing aud");
        } catch (GatewayException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new GatewayException(HttpStatus.UNAUTHORIZED, "Invalid TR");
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}

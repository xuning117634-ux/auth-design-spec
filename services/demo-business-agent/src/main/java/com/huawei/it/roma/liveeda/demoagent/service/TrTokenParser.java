package com.huawei.it.roma.liveeda.demoagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Set;

final class TrTokenParser {

    private final ObjectMapper objectMapper;

    TrTokenParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    DecodedTrContext decode(String trToken) {
        try {
            JsonNode payload = readPayload(trToken);
            String agentId = readAudience(payload);
            if (isBlank(agentId)) {
                throw new IllegalArgumentException("TR missing aud");
            }

            JsonNode agencyUser = payload.path("agency_user");
            String userJson = agencyUser.path("user").asText(null);
            if (isBlank(userJson)) {
                throw new IllegalArgumentException("TR missing agency_user.user");
            }
            JsonNode user = objectMapper.readTree(userJson);
            String userId = user.path("uid").asText(null);
            if (isBlank(userId)) {
                throw new IllegalArgumentException("TR missing agency_user.user.uid");
            }

            Set<String> permissionPointCodes = readConsentedScopes(agencyUser);
            if (permissionPointCodes.isEmpty()) {
                throw new IllegalArgumentException("TR missing agency_user.consented_scopes");
            }
            return new DecodedTrContext(agentId, userId, permissionPointCodes);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid TR payload", exception);
        }
    }

    private JsonNode readPayload(String trToken) throws java.io.IOException {
        String[] parts = trToken == null ? new String[0] : trToken.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid JWT format");
        }
        byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
        return objectMapper.readTree(new String(payloadBytes, StandardCharsets.UTF_8));
    }

    private String readAudience(JsonNode payload) {
        JsonNode audience = payload.path("aud");
        if (audience.isTextual()) {
            return audience.asText();
        }
        if (audience.isArray() && !audience.isEmpty()) {
            return audience.get(0).asText(null);
        }
        return null;
    }

    private Set<String> readConsentedScopes(JsonNode agencyUser) {
        JsonNode consentedScopes = agencyUser.path("consented_scopes");
        Set<String> permissionPointCodes = new LinkedHashSet<>();
        if (!consentedScopes.isArray()) {
            return permissionPointCodes;
        }
        consentedScopes.forEach(node -> {
            if (!node.isTextual()) {
                throw new IllegalArgumentException("TR consented_scopes must be string array");
            }
            String code = node.asText();
            if (!isBlank(code)) {
                permissionPointCodes.add(code);
            }
        });
        return permissionPointCodes;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    record DecodedTrContext(
            String agentId,
            String userId,
            Set<String> authorizedPermissionPointCodes
    ) {
    }
}

package com.huawei.it.roma.liveeda.auth.web;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public record TrCookieResolveRequest(
        @JsonAlias("agent_id")
        @NotBlank String agentId,
        @NotBlank String tr
) {
}

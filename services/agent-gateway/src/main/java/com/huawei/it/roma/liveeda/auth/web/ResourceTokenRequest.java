package com.huawei.it.roma.liveeda.auth.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ResourceTokenRequest(
        @NotBlank String agentId,
        @NotEmpty List<String> requiredTools,
        @NotBlank String returnUrl,
        @NotBlank String state
) {
}

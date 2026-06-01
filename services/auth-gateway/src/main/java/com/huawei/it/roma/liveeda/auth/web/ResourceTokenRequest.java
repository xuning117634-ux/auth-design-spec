package com.huawei.it.roma.liveeda.auth.web;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

public record ResourceTokenRequest(
        @JsonAlias("agent_id")
        @NotBlank String agentId,
        @JsonAlias("required_tools")
        @NotEmpty List<String> requiredTools,
        @JsonAlias("return_url")
        @NotBlank String returnUrl,
        @NotBlank String state,
        @JsonAlias("subject_hint")
        Map<String, String> subjectHint
) {
}

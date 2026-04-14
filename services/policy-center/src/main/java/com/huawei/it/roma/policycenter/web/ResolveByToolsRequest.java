package com.huawei.it.roma.policycenter.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ResolveByToolsRequest(
        @NotBlank String agentId,
        @NotEmpty List<String> requiredTools
) {
}

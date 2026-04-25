package com.huawei.it.roma.policycenter.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AgentPermissionPointBatchUpsertRequest(
        @NotBlank String agentId,
        @NotBlank String enterprise,
        @NotBlank String source,
        @NotNull List<String> permissionPointCodes
) {
}

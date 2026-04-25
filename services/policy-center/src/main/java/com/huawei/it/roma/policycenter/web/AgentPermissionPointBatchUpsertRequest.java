package com.huawei.it.roma.policycenter.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AgentPermissionPointBatchUpsertRequest(
        @NotBlank String agentId,
        @NotBlank String enterprise,
        @NotBlank String source,
        @NotEmpty List<@Valid Item> items
) {
    public record Item(
            @NotBlank String permissionPointCode,
            @NotBlank String status
    ) {
    }
}

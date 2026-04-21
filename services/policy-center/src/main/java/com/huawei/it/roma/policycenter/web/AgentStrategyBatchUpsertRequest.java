package com.huawei.it.roma.policycenter.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AgentStrategyBatchUpsertRequest(
        @NotBlank String agentId,
        @NotEmpty List<@Valid Item> items
) {
    public record Item(
            @NotBlank String strategyId,
            @NotBlank String permissionPointCode,
            @NotNull @Valid Condition conditions,
            @NotBlank String effect,
            @NotBlank String status
    ) {
    }

    public record Condition(
            @NotBlank String field,
            @NotBlank String operator,
            @NotEmpty List<String> values
    ) {
    }
}

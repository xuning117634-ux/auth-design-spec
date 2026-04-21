package com.huawei.it.roma.policycenter.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record PermissionPointBatchUpsertRequest(
        @NotBlank String source,
        @NotEmpty List<@Valid Item> items
) {
    public record Item(
            @NotBlank String permissionPointCode,
            @NotBlank String displayNameZh,
            @NotBlank String description,
            @NotEmpty List<String> boundTools,
            @NotBlank String status
    ) {
    }
}

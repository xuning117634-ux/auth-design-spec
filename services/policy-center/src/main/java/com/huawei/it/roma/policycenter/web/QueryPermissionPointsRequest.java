package com.huawei.it.roma.policycenter.web;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record QueryPermissionPointsRequest(
        @NotBlank String enterprise,
        String appId,
        List<String> permissionPointCodes,
        String status
) {
}

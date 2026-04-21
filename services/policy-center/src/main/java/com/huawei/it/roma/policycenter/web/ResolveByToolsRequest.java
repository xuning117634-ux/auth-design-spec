package com.huawei.it.roma.policycenter.web;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ResolveByToolsRequest(
        @NotEmpty List<String> requiredTools
) {
}

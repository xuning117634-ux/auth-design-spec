package com.huawei.it.roma.policycenter.web;

import com.huawei.it.roma.policycenter.domain.ToolItem;
import java.util.List;

public record ResolveByCodesResponse(
        List<String> policyCodes,
        List<String> allowedTools,
        List<ToolItem> toolItems
) {
}

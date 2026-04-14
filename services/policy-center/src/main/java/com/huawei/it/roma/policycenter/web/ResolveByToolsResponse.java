package com.huawei.it.roma.policycenter.web;

import com.huawei.it.roma.policycenter.domain.PolicyItem;
import java.util.List;

public record ResolveByToolsResponse(
        String agentId,
        List<String> requiredPolicyCodes,
        List<PolicyItem> policyItems
) {
}

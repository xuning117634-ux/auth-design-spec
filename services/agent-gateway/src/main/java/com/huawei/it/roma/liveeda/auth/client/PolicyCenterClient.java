package com.huawei.it.roma.liveeda.auth.client;

import java.util.Set;

public interface PolicyCenterClient {

    PolicyResolutionResult resolveByTools(String agentId, Set<String> requiredTools);
}

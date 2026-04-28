package com.huawei.it.roma.liveeda.demoagent.service;

import java.util.Set;

public interface McpGatewayClient {

    Set<String> extractAuthorizedPermissionPointCodes(String trToken);

    Set<String> resolveCoveredTools(String trToken);

    String invoke(String agentId, String trToken, Set<String> requiredTools, String message);
}

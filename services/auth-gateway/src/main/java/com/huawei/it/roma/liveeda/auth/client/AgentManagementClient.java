package com.huawei.it.roma.liveeda.auth.client;

import com.huawei.it.roma.liveeda.auth.domain.AgentRegistryEntry;

public interface AgentManagementClient {

    AgentRegistryEntry getGatewayProfile(String agentId);
}

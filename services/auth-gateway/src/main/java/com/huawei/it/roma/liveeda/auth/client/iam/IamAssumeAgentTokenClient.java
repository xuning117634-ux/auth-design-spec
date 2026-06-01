package com.huawei.it.roma.liveeda.auth.client.iam;

import com.huawei.it.roma.liveeda.auth.domain.AgentRegistryEntry;
import com.huawei.it.roma.liveeda.auth.domain.IssuedToken;

public interface IamAssumeAgentTokenClient {

    IssuedToken assumeAgentToken(AgentRegistryEntry agentRegistryEntry);
}

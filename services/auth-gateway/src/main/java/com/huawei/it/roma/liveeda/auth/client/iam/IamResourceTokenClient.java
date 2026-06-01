package com.huawei.it.roma.liveeda.auth.client.iam;

import com.huawei.it.roma.liveeda.auth.domain.AgentRegistryEntry;
import com.huawei.it.roma.liveeda.auth.domain.IssuedToken;
import com.huawei.it.roma.liveeda.auth.domain.UserAuthorizationResult;

public interface IamResourceTokenClient {

    IssuedToken issueResourceToken(
            AgentRegistryEntry agentRegistryEntry,
            UserAuthorizationResult userAuthorizationResult,
            IssuedToken agentToken
    );
}

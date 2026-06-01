package com.huawei.it.roma.liveeda.auth.client.iam;

import com.huawei.it.roma.liveeda.auth.domain.AgentRegistryEntry;
import com.huawei.it.roma.liveeda.auth.domain.IssuedToken;
import com.huawei.it.roma.liveeda.auth.domain.UserAuthorizationResult;
import com.huawei.it.roma.liveeda.auth.util.JwtTokenFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("mock")
@RequiredArgsConstructor
public class MockIamResourceTokenClient implements IamResourceTokenClient {

    private final JwtTokenFactory jwtTokenFactory;

    @Override
    public IssuedToken issueResourceToken(
            AgentRegistryEntry agentRegistryEntry,
            UserAuthorizationResult userAuthorizationResult,
            IssuedToken agentToken
    ) {
        return jwtTokenFactory.issueMockTr(agentRegistryEntry, userAuthorizationResult);
    }
}

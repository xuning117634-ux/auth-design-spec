package com.huawei.it.roma.liveeda.auth.client.iam;

import com.huawei.it.roma.liveeda.auth.domain.AgentRegistryEntry;
import com.huawei.it.roma.liveeda.auth.domain.IssuedToken;
import com.huawei.it.roma.liveeda.auth.util.JwtTokenFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("mock")
@RequiredArgsConstructor
public class MockIamAssumeAgentTokenClient implements IamAssumeAgentTokenClient {

    private final JwtTokenFactory jwtTokenFactory;

    @Override
    public IssuedToken assumeAgentToken(AgentRegistryEntry agentRegistryEntry) {
        return jwtTokenFactory.issueMockT1(agentRegistryEntry);
    }
}

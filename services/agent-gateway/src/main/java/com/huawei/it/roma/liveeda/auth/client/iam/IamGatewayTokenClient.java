package com.huawei.it.roma.liveeda.auth.client.iam;

import com.huawei.it.roma.liveeda.auth.domain.IssuedToken;

@FunctionalInterface
public interface IamGatewayTokenClient {

    IssuedToken getGatewayAgentToken();
}

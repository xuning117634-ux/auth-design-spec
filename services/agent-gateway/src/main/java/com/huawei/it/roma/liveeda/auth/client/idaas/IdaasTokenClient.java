package com.huawei.it.roma.liveeda.auth.client.idaas;

import com.huawei.it.roma.liveeda.auth.domain.BaseLoginResult;
import com.huawei.it.roma.liveeda.auth.domain.IssuedToken;
import com.huawei.it.roma.liveeda.auth.domain.UserAuthorizationResult;

public interface IdaasTokenClient {

    IssuedToken exchangeAuthorizationCode(String code, String redirectUri);

    BaseLoginResult fetchUserInfo(String accessToken);

    UserAuthorizationResult fetchAuthorizationResult(IssuedToken tc);
}

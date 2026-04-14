package com.huawei.it.roma.liveeda.auth.client.idaas;

import com.huawei.it.roma.liveeda.auth.domain.BaseLoginResult;
import com.huawei.it.roma.liveeda.auth.domain.UserAuthorizationResult;

public interface IdaasTokenClient {

    BaseLoginResult exchangeBaseLoginCode(String code);

    UserAuthorizationResult exchangeConsentCode(String code);
}

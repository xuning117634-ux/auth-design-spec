package com.huawei.it.roma.liveeda.auth.client.idaas;

import java.net.URI;
import java.util.Set;

public interface IdaasAuthorizeSupport {

    URI buildBaseAuthorizationUri(String gwState);

    URI buildConsentAuthorizationUri(String gwState, Set<String> requiredPermissionPointCodes);
}

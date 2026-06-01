package com.huawei.it.roma.liveeda.auth.client.idaas;

import java.net.URI;
import java.util.Map;
import java.util.Set;

public interface IdaasAuthorizeSupport {

    URI buildBaseAuthorizationUri(String agentId, String gwState);

    URI buildConsentAuthorizationUri(
            String agentId,
            String gwState,
            Set<String> requiredPermissionPointCodes,
            Map<String, String> subjectHint
    );
}

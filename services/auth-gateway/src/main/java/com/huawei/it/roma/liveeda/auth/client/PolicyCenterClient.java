package com.huawei.it.roma.liveeda.auth.client;

import com.huawei.it.roma.liveeda.auth.domain.AuthorizedPermissionPoint;
import java.util.List;
import java.util.Set;

public interface PolicyCenterClient {

    PolicyResolutionResult resolveByTools(Set<String> requiredTools);

    List<AuthorizedPermissionPoint> resolveByCodes(Set<String> permissionPointCodes);
}

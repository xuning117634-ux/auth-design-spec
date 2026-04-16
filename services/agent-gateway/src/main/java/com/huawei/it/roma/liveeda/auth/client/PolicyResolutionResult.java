package com.huawei.it.roma.liveeda.auth.client;

import com.huawei.it.roma.liveeda.auth.domain.AuthorizedPermissionPoint;
import java.util.List;
import java.util.Set;

public record PolicyResolutionResult(
        Set<String> requiredPermissionPointCodes,
        List<AuthorizedPermissionPoint> permissionPoints
) {
}

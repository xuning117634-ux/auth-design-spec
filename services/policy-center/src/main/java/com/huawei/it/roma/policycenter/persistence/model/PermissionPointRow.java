package com.huawei.it.roma.policycenter.persistence.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionPointRow {

    private String permissionPointCode;
    private String displayNameZh;
    private String description;
    private String status;
    private String lastSyncSource;
}

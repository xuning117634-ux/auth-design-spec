package com.huawei.it.roma.policycenter.persistence.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentPermissionPointRow {

    private String agentId;
    private String enterprise;
    private String permissionPointCode;
    private String status;
}

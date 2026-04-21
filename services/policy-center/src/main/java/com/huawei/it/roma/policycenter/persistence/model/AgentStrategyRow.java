package com.huawei.it.roma.policycenter.persistence.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentStrategyRow {

    private String strategyId;
    private String agentId;
    private String permissionPointCode;
    private String conditionField;
    private String conditionOperator;
    private String effect;
    private String status;
}

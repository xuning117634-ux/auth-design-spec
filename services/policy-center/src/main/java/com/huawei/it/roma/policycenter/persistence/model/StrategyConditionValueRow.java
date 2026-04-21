package com.huawei.it.roma.policycenter.persistence.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StrategyConditionValueRow {

    private String strategyId;
    private Integer valueOrder;
    private String conditionValue;
}

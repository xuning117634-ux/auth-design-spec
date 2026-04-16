package com.huawei.it.roma.policycenter.domain;

import java.util.List;

public record StrategyCondition(
        String field,
        String operator,
        List<String> values
) {
}

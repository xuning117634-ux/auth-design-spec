package com.huawei.it.roma.policycenter.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "policy-center.catalog")
public class PolicyCatalogProperties {

    @Valid
    @NotEmpty
    private List<PermissionPointDefinition> permissionPoints = new ArrayList<>();

    @Valid
    @NotEmpty
    private List<ToolDefinition> tools = new ArrayList<>();

    @Valid
    private List<AgentStrategyDefinition> agentStrategies = new ArrayList<>();

    @Data
    public static class PermissionPointDefinition {
        @NotBlank
        private String code;

        @NotBlank
        private String displayNameZh;

        @NotBlank
        private String description;

        @NotEmpty
        private List<String> boundTools = new ArrayList<>();

        @NotBlank
        private String status;
    }

    @Data
    public static class ToolDefinition {
        @NotBlank
        private String id;

        @NotBlank
        private String displayNameZh;
    }

    @Data
    public static class AgentStrategyDefinition {
        @NotBlank
        private String strategyId;

        @NotBlank
        private String agentId;

        @NotBlank
        private String permissionPointCode;

        @Valid
        private ConditionDefinition conditions;

        @NotBlank
        private String effect;

        @NotBlank
        private String status;
    }

    @Data
    public static class ConditionDefinition {
        @NotBlank
        private String field;

        @NotBlank
        private String operator;

        @NotEmpty
        private List<String> values = new ArrayList<>();
    }
}

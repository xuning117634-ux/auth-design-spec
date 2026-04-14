package com.huawei.it.roma.policycenter.config;

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

    @NotEmpty
    private List<PolicyDefinition> policies = new ArrayList<>();

    @NotEmpty
    private List<ToolDefinition> tools = new ArrayList<>();

    @NotEmpty
    private List<MappingDefinition> mappings = new ArrayList<>();

    @Data
    public static class PolicyDefinition {
        @NotBlank
        private String code;

        @NotBlank
        private String displayName;
    }

    @Data
    public static class ToolDefinition {
        @NotBlank
        private String id;

        @NotBlank
        private String displayName;
    }

    @Data
    public static class MappingDefinition {
        @NotBlank
        private String toolId;

        @NotBlank
        private String policyCode;
    }
}

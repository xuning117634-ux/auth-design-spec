package com.huawei.it.roma.liveeda.auth.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "mock-agent-management")
public class MockAgentManagementProperties {

    @Valid
    @NotEmpty
    private List<Entry> entries = new ArrayList<>();

    @Data
    public static class Entry {
        @NotBlank
        private String agentId;

        @NotBlank
        private String agentName;

        @NotBlank
        private String enterprise;

        @NotBlank
        private String appId;

        @NotEmpty
        private List<String> allowedReturnHosts = new ArrayList<>();

        private Set<String> subscribedPermissionPointCodes = new LinkedHashSet<>();
    }
}

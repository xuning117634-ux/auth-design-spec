package com.huawei.it.roma.liveeda.auth.config;

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
@ConfigurationProperties(prefix = "agent-registry")
public class AgentRegistryProperties {

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
        private String agentServiceAccount;

        @NotBlank
        private String principal;

        @NotEmpty
        private List<String> subscribedTools = new ArrayList<>();

        @NotEmpty
        private List<String> allowedReturnHosts = new ArrayList<>();
    }
}

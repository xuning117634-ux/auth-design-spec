package com.huawei.it.roma.liveeda.auth.config;

import jakarta.validation.constraints.NotBlank;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "clients.agent-management")
public class AgentManagementClientProperties {

    @NotBlank
    private String baseUrl;

    @NotBlank
    private String queryByAgentIdPath = "/api/dev/public/agentMall/queryByAgentId";

    private Map<String, String> headers = new LinkedHashMap<>();
}

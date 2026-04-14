package com.huawei.it.roma.liveeda.demoagent.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "demo-agent")
public class DemoAgentProperties {

    @NotBlank
    private String gatewayBaseUrl;

    @NotBlank
    private String selfBaseUrl;

    @NotBlank
    private String agentId;

    private boolean secureCookies;
}

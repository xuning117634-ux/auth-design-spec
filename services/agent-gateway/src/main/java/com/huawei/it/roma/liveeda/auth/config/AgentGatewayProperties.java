package com.huawei.it.roma.liveeda.auth.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "gateway")
public class AgentGatewayProperties {

    @NotBlank
    private String selfBaseUrl;

    private boolean secureCookies;

    @NotBlank
    private String jwtSecret;

    @NotBlank
    private String defaultUserId;

    @NotBlank
    private String defaultUsername;
}

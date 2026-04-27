package com.huawei.it.roma.liveeda.auth.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "providers.iam")
public class IamProperties {

    @NotBlank
    private String baseUrl;

    @NotBlank
    private String agentTokenPath = "/iam/auth/agent-token";

    @NotBlank
    private String assumeAgentTokenPath = "/iam/projects/{gatewayProject}/assume-agent-token";

    @NotBlank
    private String resourceTokenPath = "/iam/auth/resource-token";

    @NotBlank
    private String gatewayAccount;

    @NotBlank
    private String gatewaySecret;

    @NotBlank
    private String gatewayProject;

    @NotBlank
    private String gatewayEnterprise;

    private long gatewayTokenRefreshSkewSeconds = 300;
}

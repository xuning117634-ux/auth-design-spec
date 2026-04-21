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
    private String proxyProjectId;

    @NotBlank
    private String authorizationHeader;

    @NotBlank
    private String delegatorAccountName;

    @NotBlank
    private String delegatorAppid;

    @NotBlank
    private String agentId;
}

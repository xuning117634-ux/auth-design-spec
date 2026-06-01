package com.huawei.it.roma.liveeda.auth.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "providers.idaas")
public class IdaasProperties {

    @NotBlank
    private String authorizeUrl;

    @NotBlank
    private String tokenUrl;

    @NotBlank
    private String userinfoUrl;

    @NotBlank
    private String clientId;

    @NotBlank
    private String clientSecret;
}

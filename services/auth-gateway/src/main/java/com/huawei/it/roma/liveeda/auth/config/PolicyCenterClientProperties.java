package com.huawei.it.roma.liveeda.auth.config;

import jakarta.validation.constraints.NotBlank;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "clients.policy-center")
public class PolicyCenterClientProperties {

    @NotBlank
    private String baseUrl;

    private Map<String, String> headers = new LinkedHashMap<>();
}

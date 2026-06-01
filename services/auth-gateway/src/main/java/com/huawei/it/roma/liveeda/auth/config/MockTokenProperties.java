package com.huawei.it.roma.liveeda.auth.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Profile("mock")
@ConfigurationProperties(prefix = "mock-token")
public class MockTokenProperties {

    @NotBlank
    private String jwtSecret;
}

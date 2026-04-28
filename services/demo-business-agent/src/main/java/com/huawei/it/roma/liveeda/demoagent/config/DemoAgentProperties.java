package com.huawei.it.roma.liveeda.demoagent.config;

import jakarta.validation.constraints.NotBlank;
import java.util.LinkedHashMap;
import java.util.Map;
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
    private String policyCenterBaseUrl;

    private Map<String, String> policyCenterHeaders = new LinkedHashMap<>();

    @NotBlank
    private String selfBaseUrl;

    @NotBlank
    private String agentId;

    private boolean secureCookies;

    private Mcp mcp = new Mcp();

    @Data
    public static class Mcp {

        private String mode = "mock";

        private String gatewayBaseUrl = "http://localhost:18083";

        private String invokePath = "/internal/v1/tools/invoke";

        private Map<String, String> headers = new LinkedHashMap<>();
    }
}

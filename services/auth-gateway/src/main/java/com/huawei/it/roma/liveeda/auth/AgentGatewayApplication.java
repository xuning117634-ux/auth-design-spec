package com.huawei.it.roma.liveeda.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AgentGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentGatewayApplication.class, args);
    }
}

package com.huawei.it.roma.liveeda.demoagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DemoBusinessAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoBusinessAgentApplication.class, args);
    }
}

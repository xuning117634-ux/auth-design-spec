package com.huawei.it.roma.policycenter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PolicyCenterApplication {

    public static void main(String[] args) {
        SpringApplication.run(PolicyCenterApplication.class, args);
    }
}

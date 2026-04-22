package com.huawei.it.roma.policycenter.config;

import com.zaxxer.hikari.HikariDataSource;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DataSourceConfig {
    @Bean
    @Primary
    public DataSource dataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password-base64:}") String passwordBase64,
            @Value("${spring.datasource.driver-class-name}") String driverClassName) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(decodeBase64(passwordBase64));
        return dataSource;
    }

    private String decodeBase64(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return "";
        }
        String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        String prefix = "pc:v1:";
        String suffix = ":policy-center";
        if (!decoded.startsWith(prefix) || !decoded.endsWith(suffix) || decoded.length() <= prefix.length() + suffix.length()) {
            throw new IllegalArgumentException("Invalid spring.datasource.password-base64 format");
        }
        return decoded.substring(prefix.length(), decoded.length() - suffix.length());
    }
}

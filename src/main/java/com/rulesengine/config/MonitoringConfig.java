package com.rulesengine.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.actuate.info.Info;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class MonitoringConfig {

    @Bean
    public HealthIndicator databaseHealthIndicator(DataSource dataSource) {
        return () -> {
            try (Connection connection = dataSource.getConnection()) {
                if (connection.isValid(1)) {
                    return Health.up()
                            .withDetail("database", "Available")
                            .withDetail("validationQuery", "SELECT 1")
                            .build();
                } else {
                    return Health.down()
                            .withDetail("database", "Connection invalid")
                            .build();
                }
            } catch (Exception e) {
                return Health.down()
                        .withDetail("database", "Connection failed")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }

    @Bean
    public InfoContributor applicationInfoContributor() {
        return builder -> {
            Map<String, Object> details = new HashMap<>();
            details.put("name", "Rules Engine");
            details.put("version", "1.0.0");
            details.put("description", "Spring Boot Rules Engine Backend Service");
            details.put("startup-time", LocalDateTime.now());
            
            builder.withDetail("application", details);
        };
    }

    @Bean
    public Timer ruleExecutionTimer(MeterRegistry meterRegistry) {
        return Timer.builder("rule.execution.time")
                .description("Time taken to execute rules")
                .register(meterRegistry);
    }
}
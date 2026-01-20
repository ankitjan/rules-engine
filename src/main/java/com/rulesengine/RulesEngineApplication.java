package com.rulesengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableCaching
@EnableJpaRepositories(basePackages = "com.rulesengine.repository")
public class RulesEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(RulesEngineApplication.class, args);
    }
}
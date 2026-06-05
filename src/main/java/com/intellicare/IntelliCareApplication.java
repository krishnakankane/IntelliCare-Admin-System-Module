package com.intellicare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * IntelliCare Backend — Spring Boot 3 Application Entry Point.
 * Admin and System modules.
 */
@SpringBootApplication
@EnableAsync
@EnableCaching
@EnableScheduling
public class IntelliCareApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntelliCareApplication.class, args);
    }
}

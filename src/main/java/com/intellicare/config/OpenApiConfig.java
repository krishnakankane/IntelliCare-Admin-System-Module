package com.intellicare.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI intelliCareOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("IntelliCare Backend API")
                        .description("""
                                AI-powered healthcare platform — Admin and System modules.
                                
                                **Modules:** User & Role Management | Notifications & Alerts | Audit Logging | AI Orchestration
                                
                                All protected endpoints require a valid JWT Bearer token obtained via POST /auth/login.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("IntelliCare Team")
                                .email("dev@intellicare.com")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        ));
    }
}

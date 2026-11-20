package com.abdulrahman.university_management_system.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI universityOpenAPI() {
        String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("University Management System API")
                        .version("1.0")
                        .description(
                                "API for managing university data and enrollments."
                        ))
                .components(new Components()
                        .addSecuritySchemes(
                                securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        ))
                .addSecurityItem(
                        new SecurityRequirement().addList(securitySchemeName)
                );
    }

    @Bean
    public OpenApiCustomizer publicLoginDocumentation() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }

            PathItem loginPath = openApi.getPaths().get("/api/v1/auth/login");

            if (loginPath != null && loginPath.getPost() != null) {
                loginPath.getPost().setSecurity(List.of());
            }
        };
    }
}

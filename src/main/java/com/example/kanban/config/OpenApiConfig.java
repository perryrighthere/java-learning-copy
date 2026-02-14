package com.example.kanban.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata for Week 2 auth/RBAC scope.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Kanban Realtime API",
        version = "0.2.0",
        description = "Week 2 scope: JWT auth, board RBAC, seed users, and secured endpoints.",
        contact = @Contact(name = "Kanban Teaching Team", email = "teaching@example.com"),
        license = @License(name = "MIT")
    )
)
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}

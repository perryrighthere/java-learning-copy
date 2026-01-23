package com.example.kanban.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.context.annotation.Configuration;

/**
 * Minimal OpenAPI metadata so Swagger UI renders a useful stub during Week 1.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Kanban Realtime API",
        version = "0.1.0",
        description = "Week 1 skeleton: identity, boards, validation, and error model.",
        contact = @Contact(name = "Kanban Teaching Team", email = "teaching@example.com"),
        license = @License(name = "MIT")
    )
)
public class OpenApiConfig {
}

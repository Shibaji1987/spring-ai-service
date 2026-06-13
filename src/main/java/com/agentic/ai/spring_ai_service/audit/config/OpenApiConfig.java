package com.agentic.ai.spring_ai_service.audit.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String JWT_SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI auditPlatformOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(JWT_SECURITY_SCHEME, new SecurityScheme()
                                .name(JWT_SECURITY_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter the JWT returned by POST /auth/login.")))
                .addSecurityItem(new SecurityRequirement().addList(JWT_SECURITY_SCHEME))
                .info(new Info()
                        .title("Agentic AI Audit Microservice API")
                        .description("""
                                Policy-grounded audit event analysis using Spring AI, MongoDB, retrieval, and bounded tools.

                                Recommended analysis lifecycle:
                                1. POST /audit/events/{eventId}/analysis-runs to create one run.
                                2. GET /audit/analysis-runs/{runId}/stream to observe live progress.
                                3. GET /audit/analysis-runs/{runId} to retrieve durable status or the final result.

                                Endpoints marked deprecated are retained only for backward compatibility.
                                """)
                        .version("v2")
                        .contact(new Contact()
                                .name("Shibaji")
                                .email("your-email@example.com"))
                        .license(new License()
                                .name("Internal Use")))
                .externalDocs(new ExternalDocumentation()
                        .description("Project documentation")
                        .url("https://github.com/Shibaji1987/spring-ai-service"));
    }
}

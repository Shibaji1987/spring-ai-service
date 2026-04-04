package com.agentic.ai.spring_ai_service.audit.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI auditPlatformOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Agentic AI Audit Microservice API")
                        .description("Policy-grounded audit event analysis service using Spring AI, MongoDB, and retrieval.")
                        .version("v1")
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
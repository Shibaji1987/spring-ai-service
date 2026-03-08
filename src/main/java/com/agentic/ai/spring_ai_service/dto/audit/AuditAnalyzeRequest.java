package com.agentic.ai.spring_ai_service.dto.audit;

import jakarta.validation.constraints.NotBlank;

public record AuditAnalyzeRequest(
        @NotBlank(message = "event must not be blank")
        String event
) {}

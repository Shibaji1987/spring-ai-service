package com.agentic.ai.spring_ai_service.dto.audit;

import jakarta.validation.constraints.NotBlank;

public record AuditAnalyzeWithHistoryRequest(
        @NotBlank(message = "userId must not be blank")
        String userId,

        @NotBlank(message = "event must not be blank")
        String event
) {}
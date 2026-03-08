package com.agentic.ai.spring_ai_service.audit.dto;

public record AuditAnalyzeApiResponse(
        String event,
        long latencyMs,
        AuditAnalyzeResponse analysis
) {}
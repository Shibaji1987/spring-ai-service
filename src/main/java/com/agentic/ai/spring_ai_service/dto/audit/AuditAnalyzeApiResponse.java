package com.agentic.ai.spring_ai_service.dto.audit;

import com.agentic.ai.spring_ai_service.dto.audit.AuditAnalyzeResponse;

public record AuditAnalyzeApiResponse(
        String event,
        long latencyMs,
        AuditAnalyzeResponse analysis
) {}
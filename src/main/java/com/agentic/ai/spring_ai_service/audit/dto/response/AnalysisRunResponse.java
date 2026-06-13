package com.agentic.ai.spring_ai_service.audit.dto.response;

import java.time.Instant;

public record AnalysisRunResponse(
        String analysisRunId,
        String eventId,
        String status,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt,
        String errorMessage,
        AuditAnalysisResponseDto result,
        String streamUrl,
        String resultUrl
) {
}

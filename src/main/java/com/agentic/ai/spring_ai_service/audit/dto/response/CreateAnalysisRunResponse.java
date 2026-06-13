package com.agentic.ai.spring_ai_service.audit.dto.response;

import java.time.Instant;

public record CreateAnalysisRunResponse(
        String analysisRunId,
        String eventId,
        String status,
        Instant createdAt,
        String streamUrl,
        String resultUrl
) {
}

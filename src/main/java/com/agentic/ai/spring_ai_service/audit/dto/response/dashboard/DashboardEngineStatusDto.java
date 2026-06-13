package com.agentic.ai.spring_ai_service.audit.dto.response.dashboard;

import java.time.Instant;

public record DashboardEngineStatusDto(
        String status,
        String modelName,
        Instant lastAnalysisAt,
        long uptimeSeconds,
        Instant checkedAt
) {
}

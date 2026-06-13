package com.agentic.ai.spring_ai_service.audit.dto.response.dashboard;

import java.time.LocalDateTime;

public record RecentAuditActivityDto(
        String eventId,
        String eventType,
        String actor,
        String action,
        String target,
        String status,
        LocalDateTime eventTime,
        Integer riskScore,
        String riskLevel,
        String category,
        boolean analyzed
) {
}

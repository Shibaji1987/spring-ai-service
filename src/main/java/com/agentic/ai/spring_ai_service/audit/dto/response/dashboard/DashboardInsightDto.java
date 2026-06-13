package com.agentic.ai.spring_ai_service.audit.dto.response.dashboard;

public record DashboardInsightDto(
        String code,
        String severity,
        String title,
        String message,
        Double value,
        String unit
) {
}

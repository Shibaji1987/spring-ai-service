package com.agentic.ai.spring_ai_service.audit.dto.response.dashboard;

public record DashboardMetricDto(
        long value,
        long currentPeriodValue,
        long previousPeriodValue,
        Double changePercent,
        String trend
) {
}

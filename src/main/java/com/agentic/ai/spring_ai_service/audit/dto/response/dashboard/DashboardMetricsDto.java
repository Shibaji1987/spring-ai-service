package com.agentic.ai.spring_ai_service.audit.dto.response.dashboard;

import java.time.Instant;

public record DashboardMetricsDto(
        DashboardMetricDto totalEvents,
        DashboardMetricDto highRiskEvents,
        DashboardMetricDto aiAnalyzed,
        DashboardMetricDto policyMatches,
        Instant periodStart,
        Instant previousPeriodStart,
        Instant generatedAt
) {
}

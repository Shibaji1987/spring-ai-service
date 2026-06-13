package com.agentic.ai.spring_ai_service.audit.dto.response.dashboard;

import java.util.List;

public record DashboardSummaryDto(
        DashboardMetricsDto metrics,
        List<RecentAuditActivityDto> recentActivity,
        List<DashboardInsightDto> insights,
        DashboardEngineStatusDto engineStatus
) {
}

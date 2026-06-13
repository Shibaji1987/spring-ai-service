package com.agentic.ai.spring_ai_service.audit.controller;

import com.agentic.ai.spring_ai_service.audit.dto.response.dashboard.DashboardEngineStatusDto;
import com.agentic.ai.spring_ai_service.audit.dto.response.dashboard.DashboardInsightDto;
import com.agentic.ai.spring_ai_service.audit.dto.response.dashboard.DashboardMetricsDto;
import com.agentic.ai.spring_ai_service.audit.dto.response.dashboard.DashboardSummaryDto;
import com.agentic.ai.spring_ai_service.audit.dto.response.dashboard.RecentAuditActivityDto;
import com.agentic.ai.spring_ai_service.service.DashboardService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public DashboardSummaryDto getSummary(
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int recentLimit
    ) {
        return dashboardService.getSummary(recentLimit);
    }

    @GetMapping("/metrics")
    public DashboardMetricsDto getMetrics() {
        return dashboardService.getMetrics();
    }

    @GetMapping("/recent-activity")
    public List<RecentAuditActivityDto> getRecentActivity(
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int limit
    ) {
        return dashboardService.getRecentActivity(limit);
    }

    @GetMapping("/insights")
    public List<DashboardInsightDto> getInsights() {
        return dashboardService.getInsights();
    }

    @GetMapping("/engine-status")
    public DashboardEngineStatusDto getEngineStatus() {
        return dashboardService.getEngineStatus();
    }
}

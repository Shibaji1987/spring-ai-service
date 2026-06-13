package com.agentic.ai.spring_ai_service.service;

import com.agentic.ai.spring_ai_service.audit.dto.response.dashboard.DashboardInsightDto;
import com.agentic.ai.spring_ai_service.audit.dto.response.dashboard.DashboardMetricsDto;
import com.agentic.ai.spring_ai_service.audit.dto.response.dashboard.RecentAuditActivityDto;
import com.agentic.ai.spring_ai_service.audit.model.AuditAiAnalysis;
import com.agentic.ai.spring_ai_service.audit.model.AuditEvent;
import com.agentic.ai.spring_ai_service.audit.repository.AuditAiAnalysisRepository;
import com.agentic.ai.spring_ai_service.audit.repository.AuditEventRepository;
import com.agentic.ai.spring_ai_service.audit.repository.DashboardAnalyticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTests {

    private static final Instant NOW = Instant.parse("2026-06-13T12:00:00Z");

    @Mock
    private AuditEventRepository eventRepository;

    @Mock
    private AuditAiAnalysisRepository analysisRepository;

    @Mock
    private DashboardAnalyticsRepository analyticsRepository;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(
                eventRepository,
                analysisRepository,
                analyticsRepository,
                "gpt-4o-mini",
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void calculatesRealTotalsAndSevenDayTrends() {
        when(eventRepository.count()).thenReturn(100L);
        when(eventRepository.countByEventTimeBetween(
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        ))
                .thenReturn(20L, 10L);

        when(analyticsRepository.countAnalyzedEvents()).thenReturn(80L);
        when(analyticsRepository.countAnalyzedEventsBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(30L, 20L);
        when(analyticsRepository.countHighRiskEvents(7)).thenReturn(12L);
        when(analyticsRepository.countHighRiskEventsBetween(
                anyInt(),
                any(Instant.class),
                any(Instant.class)
        ))
                .thenReturn(5L, 4L);
        when(analyticsRepository.countPolicyMatchedEvents()).thenReturn(50L);
        when(analyticsRepository.countPolicyMatchedEventsBetween(
                any(Instant.class),
                any(Instant.class)
        ))
                .thenReturn(12L, 8L);

        DashboardMetricsDto metrics = dashboardService.getMetrics();

        assertThat(metrics.totalEvents().value()).isEqualTo(100);
        assertThat(metrics.totalEvents().changePercent()).isEqualTo(100.0);
        assertThat(metrics.totalEvents().trend()).isEqualTo("UP");
        assertThat(metrics.highRiskEvents().value()).isEqualTo(12);
        assertThat(metrics.highRiskEvents().changePercent()).isEqualTo(25.0);
        assertThat(metrics.aiAnalyzed().value()).isEqualTo(80);
        assertThat(metrics.policyMatches().value()).isEqualTo(50);
        assertThat(metrics.periodStart()).isEqualTo(Instant.parse("2026-06-06T12:00:00Z"));
        assertThat(metrics.previousPeriodStart()).isEqualTo(Instant.parse("2026-05-30T12:00:00Z"));
    }

    @Test
    void joinsRecentEventsToPersistedAnalysisAndDerivesRiskLabels() {
        AuditEvent analyzedEvent = event(
                "event-1",
                "PRIVILEGED_ACCESS",
                "analyst@bank.com",
                "EXPORT_DATA",
                "Core Banking",
                LocalDateTime.parse("2026-06-13T10:00:00")
        );
        AuditEvent unanalyzedEvent = event(
                "event-2",
                "LOGIN",
                "service-account",
                "LOGIN",
                "Payments",
                LocalDateTime.parse("2026-06-13T09:00:00")
        );
        AuditAiAnalysis analysis = AuditAiAnalysis.builder()
                .eventId("event-1")
                .riskScore(8)
                .category("admin_action")
                .analyzedAt(NOW.minusSeconds(30))
                .build();

        when(eventRepository.findAllByOrderByEventTimeDesc(PageRequest.of(0, 5)))
                .thenReturn(List.of(analyzedEvent, unanalyzedEvent));
        when(analysisRepository.findByEventIdIn(List.of("event-1", "event-2")))
                .thenReturn(List.of(analysis));

        List<RecentAuditActivityDto> activity = dashboardService.getRecentActivity(5);

        assertThat(activity).hasSize(2);
        assertThat(activity.getFirst().eventId()).isEqualTo("event-1");
        assertThat(activity.getFirst().riskLevel()).isEqualTo("HIGH");
        assertThat(activity.getFirst().analyzed()).isTrue();
        assertThat(activity.get(1).riskLevel()).isEqualTo("UNANALYZED");
        assertThat(activity.get(1).analyzed()).isFalse();
    }

    @Test
    void derivesInsightsFromCurrentAndPreviousPersistedData() {
        AuditEvent privilegedExport = event(
                "event-1",
                "LOGIN_PRIVILEGED_ACCESS",
                "admin@bank.com",
                "EXPORT_CUSTOMER_DATA",
                "Core Banking",
                LocalDateTime.parse("2026-06-12T10:00:00")
        );
        privilegedExport.setMetadata(Map.of("privilegeEscalation", true));

        when(eventRepository.findByEventTimeBetween(
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        ))
                .thenReturn(List.of(privilegedExport), List.of());
        when(analyticsRepository.countHighRiskEventsBetween(
                anyInt(),
                any(Instant.class),
                any(Instant.class)
        ))
                .thenReturn(4L);
        when(analyticsRepository.countHighRiskPolicyMatchedEventsBetween(
                anyInt(),
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(3L);

        List<DashboardInsightDto> insights = dashboardService.getInsights();

        assertThat(insights).hasSize(3);
        assertThat(insights.get(0).code()).isEqualTo("PRIVILEGED_ACCESS_TREND");
        assertThat(insights.get(0).message()).contains("recorded 1 event(s)");
        assertThat(insights.get(1).message()).contains("top actor: admin@bank.com");
        assertThat(insights.get(2).value()).isEqualTo(75.0);
        assertThat(insights.get(2).message()).contains("3 of 4");
    }

    @Test
    void reportsLatestPersistedModelAndAnalysisTime() {
        AuditAiAnalysis latest = AuditAiAnalysis.builder()
                .eventId("event-1")
                .modelName("openai-via-spring-ai")
                .analyzedAt(NOW.minusSeconds(60))
                .build();
        when(analysisRepository.findFirstByOrderByAnalyzedAtDesc()).thenReturn(Optional.of(latest));

        var status = dashboardService.getEngineStatus();

        assertThat(status.status()).isEqualTo("ACTIVE");
        assertThat(status.modelName()).isEqualTo("openai-via-spring-ai");
        assertThat(status.lastAnalysisAt()).isEqualTo(NOW.minusSeconds(60));
        assertThat(status.uptimeSeconds()).isZero();
        assertThat(status.checkedAt()).isEqualTo(NOW);
    }

    private AuditEvent event(
            String id,
            String eventType,
            String actor,
            String action,
            String target,
            LocalDateTime eventTime
    ) {
        AuditEvent event = new AuditEvent(
                eventType,
                actor,
                action,
                target,
                "SUCCESS",
                eventTime,
                Map.of()
        );
        ReflectionTestUtils.setField(event, "id", id);
        return event;
    }
}

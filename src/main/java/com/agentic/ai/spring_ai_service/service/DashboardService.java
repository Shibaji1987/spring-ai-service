package com.agentic.ai.spring_ai_service.service;

import com.agentic.ai.spring_ai_service.audit.dto.response.dashboard.DashboardEngineStatusDto;
import com.agentic.ai.spring_ai_service.audit.dto.response.dashboard.DashboardInsightDto;
import com.agentic.ai.spring_ai_service.audit.dto.response.dashboard.DashboardMetricDto;
import com.agentic.ai.spring_ai_service.audit.dto.response.dashboard.DashboardMetricsDto;
import com.agentic.ai.spring_ai_service.audit.dto.response.dashboard.DashboardSummaryDto;
import com.agentic.ai.spring_ai_service.audit.dto.response.dashboard.RecentAuditActivityDto;
import com.agentic.ai.spring_ai_service.audit.model.AuditAiAnalysis;
import com.agentic.ai.spring_ai_service.audit.model.AuditEvent;
import com.agentic.ai.spring_ai_service.audit.repository.AuditAiAnalysisRepository;
import com.agentic.ai.spring_ai_service.audit.repository.AuditEventRepository;
import com.agentic.ai.spring_ai_service.audit.repository.DashboardAnalyticsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    static final int HIGH_RISK_THRESHOLD = 7;
    private static final int PERIOD_DAYS = 7;

    private final AuditEventRepository auditEventRepository;
    private final AuditAiAnalysisRepository analysisRepository;
    private final DashboardAnalyticsRepository analyticsRepository;
    private final Clock clock;
    private final Instant startedAt;
    private final String configuredModelName;

    @Autowired
    public DashboardService(
            AuditEventRepository auditEventRepository,
            AuditAiAnalysisRepository analysisRepository,
            DashboardAnalyticsRepository analyticsRepository,
            @Value("${spring.ai.openai.chat.options.model:unknown}") String configuredModelName
    ) {
        this(
                auditEventRepository,
                analysisRepository,
                analyticsRepository,
                configuredModelName,
                Clock.systemUTC()
        );
    }

    DashboardService(
            AuditEventRepository auditEventRepository,
            AuditAiAnalysisRepository analysisRepository,
            DashboardAnalyticsRepository analyticsRepository,
            String configuredModelName,
            Clock clock
    ) {
        this.auditEventRepository = auditEventRepository;
        this.analysisRepository = analysisRepository;
        this.analyticsRepository = analyticsRepository;
        this.configuredModelName = configuredModelName;
        this.clock = clock;
        this.startedAt = clock.instant();
    }

    public DashboardSummaryDto getSummary(int recentLimit) {
        return new DashboardSummaryDto(
                getMetrics(),
                getRecentActivity(recentLimit),
                getInsights(),
                getEngineStatus()
        );
    }

    public DashboardMetricsDto getMetrics() {
        TimeWindow window = currentTimeWindow();

        long totalEvents = auditEventRepository.count();
        long currentEvents = auditEventRepository.countByEventTimeBetween(
                window.currentStartDateTime(),
                window.nowDateTime()
        );
        long previousEvents = auditEventRepository.countByEventTimeBetween(
                window.previousStartDateTime(),
                window.currentStartDateTime()
        );

        long totalAnalyses = analyticsRepository.countAnalyzedEvents();
        long currentAnalyses = analyticsRepository.countAnalyzedEventsBetween(
                window.currentStart(),
                window.now()
        );
        long previousAnalyses = analyticsRepository.countAnalyzedEventsBetween(
                window.previousStart(),
                window.currentStart()
        );

        long totalHighRisk = analyticsRepository.countHighRiskEvents(HIGH_RISK_THRESHOLD);
        long currentHighRisk = analyticsRepository.countHighRiskEventsBetween(
                HIGH_RISK_THRESHOLD,
                window.currentStart(),
                window.now()
        );
        long previousHighRisk = analyticsRepository.countHighRiskEventsBetween(
                HIGH_RISK_THRESHOLD,
                window.previousStart(),
                window.currentStart()
        );

        long totalPolicyMatches = analyticsRepository.countPolicyMatchedEvents();
        long currentPolicyMatches = analyticsRepository.countPolicyMatchedEventsBetween(
                window.currentStart(),
                window.now()
        );
        long previousPolicyMatches = analyticsRepository.countPolicyMatchedEventsBetween(
                window.previousStart(),
                window.currentStart()
        );

        return new DashboardMetricsDto(
                metric(totalEvents, currentEvents, previousEvents),
                metric(totalHighRisk, currentHighRisk, previousHighRisk),
                metric(totalAnalyses, currentAnalyses, previousAnalyses),
                metric(totalPolicyMatches, currentPolicyMatches, previousPolicyMatches),
                window.currentStart(),
                window.previousStart(),
                window.now()
        );
    }

    public List<RecentAuditActivityDto> getRecentActivity(int limit) {
        List<AuditEvent> events = auditEventRepository.findAllByOrderByEventTimeDesc(PageRequest.of(0, limit));
        if (events.isEmpty()) {
            return List.of();
        }

        List<String> eventIds = events.stream()
                .map(AuditEvent::getId)
                .toList();
        Map<String, AuditAiAnalysis> analysisByEventId = analysisRepository.findByEventIdIn(eventIds).stream()
                .collect(Collectors.toMap(
                        AuditAiAnalysis::getEventId,
                        analysis -> analysis,
                        this::mostRecent
                ));

        return events.stream()
                .map(event -> recentActivity(event, analysisByEventId.get(event.getId())))
                .toList();
    }

    public List<DashboardInsightDto> getInsights() {
        TimeWindow window = currentTimeWindow();
        List<AuditEvent> currentEvents = auditEventRepository.findByEventTimeBetween(
                window.currentStartDateTime(),
                window.nowDateTime()
        );
        List<AuditEvent> previousEvents = auditEventRepository.findByEventTimeBetween(
                window.previousStartDateTime(),
                window.currentStartDateTime()
        );

        long currentPrivileged = currentEvents.stream().filter(this::isPrivilegedAccess).count();
        long previousPrivileged = previousEvents.stream().filter(this::isPrivilegedAccess).count();
        Double privilegedChange = changePercent(currentPrivileged, previousPrivileged);

        List<AuditEvent> exportEvents = currentEvents.stream()
                .filter(this::isDataExport)
                .toList();
        String topExportActor = topActor(exportEvents);

        long currentHighRisk = analyticsRepository.countHighRiskEventsBetween(
                HIGH_RISK_THRESHOLD,
                window.currentStart(),
                window.now()
        );
        long groundedHighRisk = analyticsRepository
                .countHighRiskPolicyMatchedEventsBetween(
                        HIGH_RISK_THRESHOLD,
                        window.currentStart(),
                        window.now()
                );
        double policyMatchRate = currentHighRisk == 0
                ? 0
                : round((groundedHighRisk * 100.0) / currentHighRisk);

        return List.of(
                new DashboardInsightDto(
                        "PRIVILEGED_ACCESS_TREND",
                        currentPrivileged > previousPrivileged ? "HIGH" : "INFO",
                        "Privileged Access Trend",
                        trendMessage("Privileged access", currentPrivileged, previousPrivileged),
                        privilegedChange,
                        "percent"
                ),
                new DashboardInsightDto(
                        "DATA_EXPORT_ACTIVITY",
                        exportEvents.isEmpty() ? "INFO" : "MEDIUM",
                        "Data Export Activity",
                        exportEvents.isEmpty()
                                ? "No data export events were recorded in the last 7 days."
                                : exportEvents.size() + " data export event(s) were recorded in the last 7 days"
                                + (topExportActor == null ? "." : "; top actor: " + topExportActor + "."),
                        (double) exportEvents.size(),
                        "events"
                ),
                new DashboardInsightDto(
                        "HIGH_RISK_POLICY_MATCH_RATE",
                        policyMatchRate >= 80 ? "INFO" : "MEDIUM",
                        "High-Risk Policy Match Rate",
                        groundedHighRisk + " of " + currentHighRisk
                                + " high-risk analyses matched policy evidence in the last 7 days.",
                        policyMatchRate,
                        "percent"
                )
        );
    }

    public DashboardEngineStatusDto getEngineStatus() {
        Instant checkedAt = clock.instant();
        AuditAiAnalysis latestAnalysis = analysisRepository.findFirstByOrderByAnalyzedAtDesc().orElse(null);
        String modelName = latestAnalysis != null && hasText(latestAnalysis.getModelName())
                ? latestAnalysis.getModelName()
                : configuredModelName;

        return new DashboardEngineStatusDto(
                "ACTIVE",
                modelName,
                latestAnalysis == null ? null : latestAnalysis.getAnalyzedAt(),
                Math.max(0, ChronoUnit.SECONDS.between(startedAt, checkedAt)),
                checkedAt
        );
    }

    private DashboardMetricDto metric(long total, long current, long previous) {
        Double change = changePercent(current, previous);
        return new DashboardMetricDto(total, current, previous, change, trend(current, previous));
    }

    private Double changePercent(long current, long previous) {
        if (previous == 0) {
            return current == 0 ? 0.0 : null;
        }
        return round(((current - previous) * 100.0) / previous);
    }

    private String trend(long current, long previous) {
        if (current == previous) {
            return "FLAT";
        }
        if (previous == 0) {
            return "NEW";
        }
        return current > previous ? "UP" : "DOWN";
    }

    private String trendMessage(String label, long current, long previous) {
        if (current == previous) {
            return label + " remained flat at " + current + " event(s) versus the previous 7 days.";
        }
        if (previous == 0) {
            return label + " recorded " + current + " event(s); the previous 7-day period had none.";
        }
        return label + " " + (current > previous ? "increased" : "decreased")
                + " from " + previous + " to " + current + " event(s) versus the previous 7 days.";
    }

    private RecentAuditActivityDto recentActivity(AuditEvent event, AuditAiAnalysis analysis) {
        Integer riskScore = analysis == null ? null : analysis.getRiskScore();
        return new RecentAuditActivityDto(
                event.getId(),
                event.getEventType(),
                event.getActor(),
                event.getAction(),
                event.getTarget(),
                event.getStatus(),
                event.getEventTime(),
                riskScore,
                riskLevel(riskScore),
                analysis == null ? null : analysis.getCategory(),
                analysis != null
        );
    }

    private String riskLevel(Integer score) {
        if (score == null) {
            return "UNANALYZED";
        }
        if (score >= HIGH_RISK_THRESHOLD) {
            return "HIGH";
        }
        if (score >= 4) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private boolean isPrivilegedAccess(AuditEvent event) {
        return containsIgnoreCase(event.getEventType(), "PRIVILEGED")
                || containsIgnoreCase(event.getAction(), "PRIVILEGED")
                || booleanMetadata(event, "privilegeEscalation");
    }

    private boolean isDataExport(AuditEvent event) {
        return containsIgnoreCase(event.getEventType(), "EXPORT")
                || containsIgnoreCase(event.getAction(), "EXPORT");
    }

    private String topActor(List<AuditEvent> events) {
        return events.stream()
                .map(AuditEvent::getActor)
                .filter(this::hasText)
                .collect(Collectors.groupingBy(actor -> actor, Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.<String, Long>comparingByValue()
                        .thenComparing(Map.Entry.comparingByKey(Comparator.naturalOrder())))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private AuditAiAnalysis mostRecent(AuditAiAnalysis first, AuditAiAnalysis second) {
        if (first.getAnalyzedAt() == null) {
            return second;
        }
        if (second.getAnalyzedAt() == null) {
            return first;
        }
        return first.getAnalyzedAt().isAfter(second.getAnalyzedAt()) ? first : second;
    }

    private boolean booleanMetadata(AuditEvent event, String key) {
        if (event.getMetadata() == null) {
            return false;
        }
        Object value = event.getMetadata().get(key);
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    private boolean containsIgnoreCase(String value, String expected) {
        return value != null && value.toUpperCase(Locale.ROOT).contains(expected);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private TimeWindow currentTimeWindow() {
        Instant now = clock.instant();
        Instant currentStart = now.minus(PERIOD_DAYS, ChronoUnit.DAYS);
        Instant previousStart = currentStart.minus(PERIOD_DAYS, ChronoUnit.DAYS);
        return new TimeWindow(
                now,
                currentStart,
                previousStart,
                LocalDateTime.ofInstant(now, ZoneOffset.UTC),
                LocalDateTime.ofInstant(currentStart, ZoneOffset.UTC),
                LocalDateTime.ofInstant(previousStart, ZoneOffset.UTC)
        );
    }

    private record TimeWindow(
            Instant now,
            Instant currentStart,
            Instant previousStart,
            LocalDateTime nowDateTime,
            LocalDateTime currentStartDateTime,
            LocalDateTime previousStartDateTime
    ) {
    }
}

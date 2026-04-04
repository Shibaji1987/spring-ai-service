package com.agentic.ai.spring_ai_service.audit.tools;

import com.agentic.ai.spring_ai_service.audit.model.AuditAiAnalysis;
import com.agentic.ai.spring_ai_service.audit.model.AuditEvent;
import com.agentic.ai.spring_ai_service.audit.repository.AuditAiAnalysisRepository;
import com.agentic.ai.spring_ai_service.service.AuditEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AuditTools {

    private static final Logger log = LoggerFactory.getLogger(AuditTools.class);

    private final AuditEventService auditEventService;
    private final AuditAiAnalysisRepository auditAiAnalysisRepository;

    public AuditTools(AuditEventService auditEventService,
                      AuditAiAnalysisRepository auditAiAnalysisRepository) {
        this.auditEventService = auditEventService;
        this.auditAiAnalysisRepository = auditAiAnalysisRepository;
    }

    @Tool(description = "Fetch the most recent audit events for an actor.")
    public List<String> getRecentEvents(
            @ToolParam(description = "Actor username or id") String actor,
            @ToolParam(description = "Max number of events to return, between 1 and 10") int limit
    ) {
        int safeLimit = Math.max(1, Math.min(limit, 10));
        log.info("[AGENT][TOOL] getRecentEvents actor={} limit={}", actor, safeLimit);

        List<String> results = auditEventService.getEventsByActor(actor).stream()
                .sorted(Comparator.comparing(AuditEvent::getEventTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(safeLimit)
                .map(this::toCompactEventText)
                .toList();

        log.info("[AGENT][TOOL] getRecentEvents returned {} rows", results.size());
        return results;
    }

    @Tool(description = "Fetch previous high risk analyses for an actor based on stored AI analysis.")
    public List<String> getPreviousHighRiskEvents(
            @ToolParam(description = "Actor username or id") String actor,
            @ToolParam(description = "Minimum risk score threshold, e.g. 7") int minRiskScore
    ) {
        int safeThreshold = Math.max(1, Math.min(minRiskScore, 10));
        log.info("[AGENT][TOOL] getPreviousHighRiskEvents actor={} minRiskScore={}", actor, safeThreshold);

        Map<String, AuditEvent> eventMap = auditEventService.getEventsByActor(actor).stream()
                .collect(Collectors.toMap(AuditEvent::getId, e -> e, (a, b) -> a));

        List<String> results = auditAiAnalysisRepository.findAll().stream()
                .filter(a -> a.getEventId() != null)
                .filter(a -> a.getRiskScore() >= safeThreshold)
                .filter(a -> eventMap.containsKey(a.getEventId()))
                .sorted(Comparator.comparing(AuditAiAnalysis::getRiskScore).reversed())
                .limit(5)
                .map(a -> {
                    AuditEvent e = eventMap.get(a.getEventId());
                    return "eventType=" + e.getEventType()
                            + ", action=" + e.getAction()
                            + ", target=" + e.getTarget()
                            + ", status=" + e.getStatus()
                            + ", riskScore=" + a.getRiskScore()
                            + ", category=" + a.getCategory()
                            + ", summary=" + a.getSummary();
                })
                .toList();

        log.info("[AGENT][TOOL] getPreviousHighRiskEvents returned {} rows", results.size());
        return results;
    }

    @Tool(description = "Return failed login count estimate for an actor based on metadata in recent login events.")
    public Integer getFailedLoginCount(
            @ToolParam(description = "Actor username or id") String actor
    ) {
        log.info("[AGENT][TOOL] getFailedLoginCount actor={}", actor);

        int count = auditEventService.getEventsByActor(actor).stream()
                .filter(e -> "LOGIN".equalsIgnoreCase(e.getEventType()))
                .map(AuditEvent::getMetadata)
                .filter(m -> m != null && m.get("failedAttempts") != null)
                .mapToInt(m -> {
                    Object value = m.get("failedAttempts");
                    if (value instanceof Number n) {
                        return n.intValue();
                    }
                    try {
                        return Integer.parseInt(String.valueOf(value));
                    } catch (Exception ex) {
                        return 0;
                    }
                })
                .sum();

        log.info("[AGENT][TOOL] getFailedLoginCount returned {}", count);
        return count;
    }

    @Tool(description = "Return a compact user activity summary for the actor.")
    public String getUserActivitySummary(
            @ToolParam(description = "Actor username or id") String actor
    ) {
        log.info("[AGENT][TOOL] getUserActivitySummary actor={}", actor);

        List<AuditEvent> events = auditEventService.getEventsByActor(actor);
        long total = events.size();
        long successful = events.stream().filter(e -> "SUCCESS".equalsIgnoreCase(e.getStatus())).count();
        long failed = events.stream().filter(e -> "FAILED".equalsIgnoreCase(e.getStatus())).count();

        String summary = "actor=" + actor
                + ", totalEvents=" + total
                + ", successCount=" + successful
                + ", failureCount=" + failed;

        log.info("[AGENT][TOOL] getUserActivitySummary returned summary={}", summary);
        return summary;
    }

    private String toCompactEventText(AuditEvent event) {
        return "eventType=" + event.getEventType()
                + ", action=" + event.getAction()
                + ", target=" + event.getTarget()
                + ", status=" + event.getStatus()
                + ", eventTime=" + event.getEventTime()
                + ", metadata=" + event.getMetadata();
    }
}
package com.agentic.ai.spring_ai_service.audit.tools;

import com.agentic.ai.spring_ai_service.audit.repository.AuditAiAnalysisRepository;
import com.agentic.ai.spring_ai_service.audit.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditTools {

    private final AuditEventRepository auditEventRepository;
    private final AuditAiAnalysisRepository auditAiAnalysisRepository;

    @Tool(description = "Get a summary of the user's recent activity history.")
    public String getUserActivitySummary(String actor) {
        log.info("[AGENT][TOOL] getUserActivitySummary actor={}", actor);

        var events = auditEventRepository.findByActor(actor);
        long successCount = events.stream().filter(e -> "SUCCESS".equalsIgnoreCase(e.getStatus())).count();
        long failureCount = events.stream().filter(e -> "FAILURE".equalsIgnoreCase(e.getStatus())).count();

        String result = "actor=%s, totalEvents=%d, successCount=%d, failureCount=%d"
                .formatted(actor, events.size(), successCount, failureCount);

        log.info("[AGENT][TOOL] getUserActivitySummary returned summary={}", result);
        return result;
    }

    @Tool(description = "Get the failed login count for the given actor.")
    public Integer getFailedLoginCount(String actor) {
        log.info("[AGENT][TOOL] getFailedLoginCount actor={}", actor);

        int count = (int) auditEventRepository.findByActor(actor).stream()
                .filter(e -> "FAILURE".equalsIgnoreCase(e.getStatus()))
                .filter(e -> "LOGIN".equalsIgnoreCase(e.getEventType()) || "USER_LOGIN".equalsIgnoreCase(e.getAction()))
                .count();

        log.info("[AGENT][TOOL] getFailedLoginCount returned {}", count);
        return count;
    }

    @Tool(description = "Get the most recent audit events for the actor. Use when recent activity context is needed.")
    public String getRecentEvents(String actor, Integer limit) {
        int safeLimit = (limit == null || limit <= 0) ? 5 : Math.min(limit, 10);
        log.info("[AGENT][TOOL] getRecentEvents actor={} limit={}", actor, safeLimit);

        var events = auditEventRepository.findByActor(actor).stream()
                .sorted((a, b) -> b.getEventTime().compareTo(a.getEventTime()))
                .limit(safeLimit)
                .toList();

        String result = events.stream()
                .map(e -> "[eventType=%s, action=%s, status=%s, target=%s, time=%s]"
                        .formatted(e.getEventType(), e.getAction(), e.getStatus(), e.getTarget(), e.getEventTime()))
                .reduce((a, b) -> a + ", " + b)
                .orElse("No recent events found");

        log.info("[AGENT][TOOL] getRecentEvents returned {} rows", events.size());
        return result;
    }
}
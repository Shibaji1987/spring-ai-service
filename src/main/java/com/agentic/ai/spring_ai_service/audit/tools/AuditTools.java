package com.agentic.ai.spring_ai_service.audit.tools;

import com.agentic.ai.spring_ai_service.audit.model.AuditAiAnalysis;
import com.agentic.ai.spring_ai_service.audit.model.AuditEvent;

import com.agentic.ai.spring_ai_service.audit.repository.AuditAiAnalysisRepository;
import com.agentic.ai.spring_ai_service.audit.repository.AuditEventRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AuditTools {

    private final AuditEventRepository auditEventRepository;
    private final AuditAiAnalysisRepository auditAiAnalysisRepository;

    public AuditTools(AuditEventRepository auditEventRepository,
                      AuditAiAnalysisRepository auditAiAnalysisRepository) {
        this.auditEventRepository = auditEventRepository;
        this.auditAiAnalysisRepository = auditAiAnalysisRepository;
    }

    @Tool(description = "Get the recent audit events for a given user id or actor")
    public List<Map<String, Object>> getRecentEvents(String userId) {
        return auditEventRepository.findByActor(userId).stream()
                .sorted((a, b) -> b.getEventTime().compareTo(a.getEventTime()))
                .limit(10)
                .map(this::toEventMap)
                .collect(Collectors.toList());
    }

    @Tool(description = "Get high risk AI audit analysis entries for a given user id or actor")
    public List<Map<String, Object>> getHighRiskEvents(String userId) {
        List<String> eventIds = auditEventRepository.findByActor(userId).stream()
                .map(AuditEvent::getId)
                .collect(Collectors.toList());

        return eventIds.stream()
                .map(auditAiAnalysisRepository::findByAuditEventId)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .filter(analysis -> analysis.getRiskScore() != null && analysis.getRiskScore() >= 7)
                .sorted((a, b) -> Integer.compare(
                        b.getRiskScore() != null ? b.getRiskScore() : 0,
                        a.getRiskScore() != null ? a.getRiskScore() : 0
                ))
                .limit(10)
                .map(this::toAnalysisMap)
                .collect(Collectors.toList());
    }

    @Tool(description = "Get failed login or failed status events for a given user id or actor")
    public List<Map<String, Object>> getFailedLogins(String userId) {
        return auditEventRepository.findByActor(userId).stream()
                .filter(event -> event.getStatus() != null && event.getStatus().equalsIgnoreCase("FAILED"))
                .sorted((a, b) -> b.getEventTime().compareTo(a.getEventTime()))
                .limit(10)
                .map(this::toEventMap)
                .collect(Collectors.toList());
    }

    @Tool(description = "Get a user security summary based on audit and AI analysis data")
    public Map<String, Object> getUserSecurityProfile(String userId) {
        List<AuditEvent> events = auditEventRepository.findByActor(userId);

        long totalEvents = events.size();
        long failedEvents = events.stream()
                .filter(event -> event.getStatus() != null && event.getStatus().equalsIgnoreCase("FAILED"))
                .count();

        List<AuditAiAnalysis> analyses = events.stream()
                .map(AuditEvent::getId)
                .map(auditAiAnalysisRepository::findByAuditEventId)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .toList();

        long highRiskCount = analyses.stream()
                .filter(a -> a.getRiskScore() != null && a.getRiskScore() >= 7)
                .count();

        double avgRiskScore = analyses.stream()
                .filter(a -> a.getRiskScore() != null)
                .mapToInt(AuditAiAnalysis::getRiskScore)
                .average()
                .orElse(0.0);

        AuditEvent latestEvent = events.stream()
                .sorted((a, b) -> b.getEventTime().compareTo(a.getEventTime()))
                .findFirst()
                .orElse(null);

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("userId", userId);
        profile.put("totalEvents", totalEvents);
        profile.put("failedEvents", failedEvents);
        profile.put("highRiskEvents", highRiskCount);
        profile.put("averageRiskScore", avgRiskScore);
        profile.put("latestEventTime", latestEvent != null ? latestEvent.getEventTime() : null);
        profile.put("latestEventType", latestEvent != null ? latestEvent.getEventType() : null);

        return profile;
    }

    private Map<String, Object> toEventMap(AuditEvent event) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", event.getId());
        map.put("eventType", event.getEventType());
        map.put("actor", event.getActor());
        map.put("action", event.getAction());
        map.put("target", event.getTarget());
        map.put("status", event.getStatus());
        map.put("eventTime", event.getEventTime());
        map.put("metadata", event.getMetadata());
        return map;
    }

    private Map<String, Object> toAnalysisMap(AuditAiAnalysis analysis) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", analysis.getId());
        map.put("auditEventId", analysis.getAuditEventId());
        map.put("riskScore", analysis.getRiskScore());
        map.put("category", analysis.getCategory());
        map.put("summary", analysis.getSummary());
        map.put("reasons", analysis.getReasons());
        map.put("tags", analysis.getTags());
        map.put("recommendedAction", analysis.getRecommendedAction());
        return map;
    }
}
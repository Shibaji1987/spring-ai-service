package com.agentic.ai.spring_ai_service.audit.tools;

import com.agentic.ai.spring_ai_service.audit.model.AuditEvent;
import com.agentic.ai.spring_ai_service.audit.model.KnowledgeChunk;
import com.agentic.ai.spring_ai_service.audit.repository.AuditEventRepository;
import com.agentic.ai.spring_ai_service.service.KnowledgeRetrievalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvestigationEvidenceService {

    private static final int MAX_EVENTS = 20;
    private static final int MAX_POLICIES = 5;

    private final AuditEventRepository auditEventRepository;
    private final KnowledgeRetrievalService knowledgeRetrievalService;

    public InvestigationEvidence identityRisk(AuditEvent event) {
        Map<String, Object> facts = selectedMetadata(event,
                "identityStatus", "employmentStatus", "accountStatus", "role", "roles",
                "privilegeLevel", "isPrivileged", "mfaEnabled", "breakGlass", "manager", "department");
        facts.put("actor", safe(event.getActor()));
        return evidence("audit_event.identity", event, facts,
                missing(facts, "identityStatus", "employmentStatus", "role", "mfaEnabled"));
    }

    public InvestigationEvidence authenticationRisk(AuditEvent event) {
        List<AuditEvent> history = actorHistory(event);
        long failedLogins = history.stream()
                .filter(this::isAuthenticationEvent)
                .filter(item -> "FAILURE".equalsIgnoreCase(item.getStatus()))
                .count();

        Map<String, Object> facts = selectedMetadata(event,
                "authenticationMethod", "mfaResult", "mfaEnabled", "credentialAgeDays",
                "passwordResetAt", "loginRisk", "newDevice", "impossibleTravel");
        facts.put("historicalFailedLogins", failedLogins);
        facts.put("eventStatus", safe(event.getStatus()));
        return evidence("audit_events.authentication", event, facts,
                missing(facts, "authenticationMethod", "mfaResult"));
    }

    public InvestigationEvidence behavioralBaseline(AuditEvent event) {
        List<AuditEvent> history = actorHistory(event).stream()
                .filter(item -> !Objects.equals(item.getId(), event.getId()))
                .toList();

        Map<String, Long> actionFrequency = frequency(history, AuditEvent::getAction);
        Map<String, Long> targetFrequency = frequency(history, AuditEvent::getTarget);
        Map<String, Long> locationFrequency = frequency(history, item -> metadataString(item, "location", "geoLocation", "city", "country"));

        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("historicalEventCount", history.size());
        facts.put("actionSeenBefore", actionFrequency.containsKey(normalize(event.getAction())));
        facts.put("targetSeenBefore", targetFrequency.containsKey(normalize(event.getTarget())));
        facts.put("actionFrequency", actionFrequency.getOrDefault(normalize(event.getAction()), 0L));
        facts.put("targetFrequency", targetFrequency.getOrDefault(normalize(event.getTarget()), 0L));
        facts.put("usualLocations", topKeys(locationFrequency, 5));
        facts.put("eventHour", event.getEventTime() == null ? null : event.getEventTime().getHour());
        return evidence("audit_events.behavioral_baseline", event, facts,
                history.isEmpty() ? List.of("No historical events are available for this actor.") : List.of());
    }

    public InvestigationEvidence relatedEventSequence(AuditEvent event, int hours) {
        LocalDateTime eventTime = event.getEventTime() == null ? LocalDateTime.now() : event.getEventTime();
        LocalDateTime from = eventTime.minusHours(hours);
        LocalDateTime to = eventTime.plusHours(hours);

        List<Map<String, Object>> timeline = auditEventRepository.findByEventTimeBetween(from, to).stream()
                .filter(item -> same(item.getActor(), event.getActor()) || same(item.getTarget(), event.getTarget()))
                .sorted((left, right) -> compareTimes(left.getEventTime(), right.getEventTime()))
                .limit(MAX_EVENTS)
                .map(this::eventSummary)
                .toList();

        return evidence("audit_events.related_sequence", event,
                Map.of("windowHours", hours, "eventCount", timeline.size(), "timeline", timeline),
                timeline.isEmpty() ? List.of("No related events were found in the bounded time window.") : List.of());
    }

    public InvestigationEvidence assetRisk(AuditEvent event) {
        Map<String, Object> facts = selectedMetadata(event,
                "assetCriticality", "environment", "assetOwner", "businessService", "dataClassification",
                "internetExposed", "regulatedData", "systemTier", "resourceType");
        facts.put("target", safe(event.getTarget()));
        return evidence("audit_event.asset", event, facts,
                missing(facts, "assetCriticality", "environment", "dataClassification"));
    }

    public InvestigationEvidence networkRisk(AuditEvent event) {
        Map<String, Object> facts = selectedMetadata(event,
                "ipAddress", "ip", "sourceIp", "location", "geoLocation", "country", "city",
                "asn", "vpn", "proxy", "tor", "ipReputation", "impossibleTravel", "networkZone");
        return evidence("audit_event.network", event, facts,
                missing(facts, "ipAddress", "sourceIp", "ipReputation", "location"));
    }

    public InvestigationEvidence sessionRisk(AuditEvent event) {
        Map<String, Object> facts = selectedMetadata(event,
                "sessionId", "deviceId", "deviceFingerprint", "deviceManaged", "userAgent",
                "tokenAgeSeconds", "tokenIssuedAt", "authenticationMethod", "concurrentSessions",
                "sessionRisk", "newDevice");
        return evidence("audit_event.session", event, facts,
                missing(facts, "sessionId", "deviceId", "tokenAgeSeconds"));
    }

    public InvestigationEvidence authorizationContext(AuditEvent event) {
        Map<String, Object> facts = selectedMetadata(event,
                "approved", "approvalId", "approvedBy", "ticketId", "changeRequestId",
                "maintenanceWindow", "entitlement", "entitlementSource", "breakGlass",
                "breakGlassReason", "separationOfDutiesViolation");
        return evidence("audit_event.authorization", event, facts,
                missing(facts, "approved", "ticketId", "entitlement"));
    }

    public InvestigationEvidence dataExposure(AuditEvent event) {
        Map<String, Object> facts = selectedMetadata(event,
                "recordCount", "bytesTransferred", "exportFormat", "destination", "destinationType",
                "dataClassification", "containsPii", "containsPci", "containsPhi", "encrypted",
                "dlpResult", "externalRecipient");
        facts.put("action", safe(event.getAction()));
        return evidence("audit_event.data_exposure", event, facts,
                missing(facts, "recordCount", "bytesTransferred", "dataClassification", "destination"));
    }

    public InvestigationEvidence applicablePolicies(AuditEvent event) {
        String query = "%s %s %s %s %s %s".formatted(
                safe(event.getEventType()), safe(event.getAction()), safe(event.getTarget()),
                metadataString(event, "role", "privilegeLevel"),
                metadataString(event, "dataClassification", "regulatedData"),
                event.getMetadata() == null ? "" : event.getMetadata());

        List<KnowledgeChunk> chunks = knowledgeRetrievalService.findTopKRelevantChunks(query, MAX_POLICIES);
        List<Map<String, Object>> matches = chunks.stream()
                .map(chunk -> Map.<String, Object>of(
                        "chunkId", safe(chunk.getId()),
                        "documentId", safe(chunk.getDocumentId()),
                        "title", safe(chunk.getDocumentTitle()),
                        "excerpt", truncate(chunk.getText(), 360)))
                .toList();

        List<String> evidenceIds = chunks.stream().map(KnowledgeChunk::getId).filter(Objects::nonNull).toList();
        return InvestigationEvidence.of(
                "knowledge_chunks.vector_search",
                matches.isEmpty() ? 0.0 : 0.85,
                evidenceIds,
                Map.of("query", query, "matchCount", matches.size(), "matches", matches),
                matches.isEmpty() ? List.of("No embedded policy chunks matched the event.") : List.of(
                        "Similarity scores are not persisted by the current in-memory vector search."));
    }

    public InvestigationEvidence controlCoverage(AuditEvent event) {
        Map<String, Object> facts = selectedMetadata(event,
                "mfaEnabled", "mfaResult", "pamManaged", "edrPresent", "dlpResult", "siemAlertId",
                "approvalId", "ticketId", "encryptionEnabled", "loggingEnabled", "auditDisabled",
                "controlExceptions", "compensatingControls");
        long presentControls = facts.values().stream().filter(this::isPositiveControlValue).count();
        facts.put("recordedControlSignalCount", presentControls);
        return evidence("audit_event.controls", event, facts,
                facts.size() <= 1 ? List.of("The event contains no explicit control telemetry.") : List.of());
    }

    public InvestigationEvidence threatIndicators(AuditEvent event) {
        Map<String, Object> facts = selectedMetadata(event,
                "ipReputation", "threatScore", "threatIntelMatches", "malwareFamily", "domainReputation",
                "fileHash", "indicatorIds", "userAgentRisk", "knownBad", "tor", "proxy");
        return evidence("audit_event.threat_intelligence", event, facts, List.of(
                "No live threat-intelligence provider is configured; findings only reflect event-enriched telemetry."));
    }

    private InvestigationEvidence evidence(
            String source,
            AuditEvent event,
            Map<String, Object> facts,
            List<String> limitations
    ) {
        double confidence = facts.size() <= 2 ? 0.45 : Math.min(0.95, 0.55 + (facts.size() * 0.04));
        List<String> evidenceIds = event.getId() == null ? List.of() : List.of(event.getId());
        return InvestigationEvidence.of(source, confidence, evidenceIds, facts, limitations);
    }

    private List<AuditEvent> actorHistory(AuditEvent event) {
        if (event.getActor() == null || event.getActor().isBlank()) {
            return List.of();
        }
        return auditEventRepository.findByActor(event.getActor()).stream().limit(200).toList();
    }

    private Map<String, Object> selectedMetadata(AuditEvent event, String... keys) {
        Map<String, Object> selected = new LinkedHashMap<>();
        Map<String, Object> metadata = event.getMetadata() == null ? Map.of() : event.getMetadata();
        for (String key : keys) {
            if (metadata.containsKey(key) && metadata.get(key) != null) {
                selected.put(key, metadata.get(key));
            }
        }
        return selected;
    }

    private List<String> missing(Map<String, Object> facts, String... keys) {
        List<String> missing = new ArrayList<>();
        for (String key : keys) {
            if (!facts.containsKey(key)) {
                missing.add(key);
            }
        }
        return missing.isEmpty() ? List.of() : List.of("Missing optional telemetry: " + String.join(", ", missing));
    }

    private Map<String, Long> frequency(List<AuditEvent> events, Function<AuditEvent, String> extractor) {
        return events.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .map(this::normalize)
                .filter(value -> !value.isBlank())
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
    }

    private List<String> topKeys(Map<String, Long> frequency, int limit) {
        return frequency.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }

    private Map<String, Object> eventSummary(AuditEvent event) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("eventId", safe(event.getId()));
        summary.put("eventType", safe(event.getEventType()));
        summary.put("actor", safe(event.getActor()));
        summary.put("action", safe(event.getAction()));
        summary.put("target", safe(event.getTarget()));
        summary.put("status", safe(event.getStatus()));
        summary.put("eventTime", event.getEventTime());
        return summary;
    }

    private boolean isAuthenticationEvent(AuditEvent event) {
        String value = (safe(event.getEventType()) + " " + safe(event.getAction())).toUpperCase(Locale.ROOT);
        return value.contains("LOGIN") || value.contains("AUTH");
    }

    private boolean isPositiveControlValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        String normalized = normalize(String.valueOf(value));
        return !normalized.isBlank()
                && !List.of("false", "failed", "disabled", "none", "no", "unknown").contains(normalized);
    }

    private String metadataString(AuditEvent event, String... keys) {
        if (event.getMetadata() == null) {
            return "";
        }
        for (String key : keys) {
            Object value = event.getMetadata().get(key);
            if (value != null) {
                return String.valueOf(value);
            }
        }
        return "";
    }

    private int compareTimes(LocalDateTime left, LocalDateTime right) {
        if (left == null && right == null) return 0;
        if (left == null) return 1;
        if (right == null) return -1;
        return left.compareTo(right);
    }

    private boolean same(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private String normalize(String value) {
        return safe(value).trim().toLowerCase(Locale.ROOT);
    }

    private String truncate(String value, int maxLength) {
        String safeValue = safe(value);
        return safeValue.length() <= maxLength ? safeValue : safeValue.substring(0, maxLength);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}

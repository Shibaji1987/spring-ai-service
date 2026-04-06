package com.agentic.ai.spring_ai_service.service;

import com.agentic.ai.spring_ai_service.audit.dto.agent.AgentFinalizePayload;
import com.agentic.ai.spring_ai_service.audit.model.AuditEvent;
import com.agentic.ai.spring_ai_service.audit.model.MatchedPolicyEvidence;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditAiService {

    private final ChatClient.Builder chatClientBuilder;

    /*
     * Backward-compatible legacy method used by older services.
     */
    public String analyze(AuditEvent event) {
        AgentFinalizePayload payload = buildDeterministicPayload(event);

        return """
                {
                  "riskScore": %d,
                  "category": "%s",
                  "summary": "%s"
                }
                """.formatted(
                payload.getRiskScore(),
                payload.getCategory(),
                escapeJson(payload.getSummary())
        );
    }

    /*
     * Backward-compatible legacy method used by orchestrator call sites
     * that still expect generateFinalAnalysis(...).
     */
    public AgentFinalizePayload generateFinalAnalysis(
            AuditEvent event,
            List<MatchedPolicyEvidence> evidence,
            List<String> observations,
            boolean fallbackUsed
    ) {
        AgentFinalizePayload baseline = buildDeterministicPayload(event);
        baseline.setFallbackUsed(fallbackUsed);
        return enrichFinalAnalysis(event, baseline);
    }

    /*
     * New preferred method.
     * Enriches baseline payload but never downgrades risk/category.
     */
    public AgentFinalizePayload enrichFinalAnalysis(
            AuditEvent event,
            AgentFinalizePayload baseline
    ) {
        if (baseline == null) {
            baseline = AgentFinalizePayload.builder()
                    .riskScore(5)
                    .category("REVIEW_REQUIRED")
                    .summary("Review required.")
                    .reasons(List.of("baseline_missing"))
                    .tags(List.of("fallback"))
                    .recommendedAction("Review event manually.")
                    .fallbackUsed(true)
                    .build();
        }

        AgentFinalizePayload safeBaseline = copyPayload(baseline);

        try {
            ChatClient chatClient = chatClientBuilder.build();

            String prompt = """
                    You are improving an existing audit analysis.

                    Rules:
                    1. Do not reduce the risk score.
                    2. Do not change the category.
                    3. Improve only the summary wording.
                    4. Return plain text only, not JSON.

                    Existing category: %s
                    Existing risk score: %d
                    Existing summary: %s
                    Event type: %s
                    Actor: %s
                    Action: %s
                    Target: %s
                    Status: %s
                    Metadata: %s
                    """.formatted(
                    safe(safeBaseline.getCategory()),
                    safeBaseline.getRiskScore() == null ? 0 : safeBaseline.getRiskScore(),
                    safe(safeBaseline.getSummary()),
                    safe(event != null ? event.getEventType() : null),
                    safe(event != null ? event.getActor() : null),
                    safe(event != null ? event.getAction() : null),
                    safe(event != null ? event.getTarget() : null),
                    safe(event != null ? event.getStatus() : null),
                    String.valueOf(event != null ? event.getMetadata() : null)
            );

            String improvedSummary = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            if (improvedSummary != null && !improvedSummary.isBlank()) {
                safeBaseline.setSummary(improvedSummary.trim());
            }

            log.info(
                    "[LLM-FINAL] enrichment successful baselineRisk={} baselineCategory={}",
                    safeBaseline.getRiskScore(),
                    safeBaseline.getCategory()
            );

            return safeBaseline;

        } catch (Exception ex) {
            log.warn(
                    "[LLM-FINAL] enrichment failed, using baseline. error={}",
                    ex.getMessage()
            );
            return safeBaseline;
        }
    }

    private AgentFinalizePayload buildDeterministicPayload(AuditEvent event) {
        boolean failed = isFailed(event);
        boolean suspicious = isSuspicious(event);
        boolean adminAction = isAdminAction(event);

        if (adminAction) {
            return AgentFinalizePayload.builder()
                    .riskScore(8)
                    .category("UNUSUAL_ADMIN_ACTION")
                    .summary("Unusual admin activity detected and requires review.")
                    .reasons(List.of("admin_action_detected"))
                    .tags(List.of("admin", "review"))
                    .recommendedAction("Review the admin action and confirm authorization.")
                    .fallbackUsed(false)
                    .build();
        }

        if (suspicious) {
            return AgentFinalizePayload.builder()
                    .riskScore(failed ? 6 : 5)
                    .category("SUSPICIOUS_LOGIN")
                    .summary("Suspicious login activity detected.")
                    .reasons(defaultSuspiciousReasons(event))
                    .tags(List.of("login", "security", "review"))
                    .recommendedAction("Investigate the login attempt and verify the user's identity.")
                    .fallbackUsed(false)
                    .build();
        }

        if (isLogin(event)) {
            return AgentFinalizePayload.builder()
                    .riskScore(2)
                    .category("BENIGN_LOGIN")
                    .summary("Normal login activity.")
                    .reasons(List.of("successful_login"))
                    .tags(List.of("login"))
                    .recommendedAction("No action required.")
                    .fallbackUsed(false)
                    .build();
        }

        return AgentFinalizePayload.builder()
                .riskScore(4)
                .category("REVIEW_REQUIRED")
                .summary("Manual review required.")
                .reasons(List.of("generic_review"))
                .tags(List.of("review"))
                .recommendedAction("Review manually.")
                .fallbackUsed(false)
                .build();
    }

    private List<String> defaultSuspiciousReasons(AuditEvent event) {
        Set<String> reasons = new LinkedHashSet<>();
        reasons.add("suspicious_login_detected");

        if (isFailed(event)) {
            reasons.add("failed_login");
        }
        if (hasBooleanMetadata(event, "geoAnomaly")) {
            reasons.add("geo_anomaly");
        }
        if (!readBooleanMetadata(event, "knownDevice", true)) {
            reasons.add("unknown_device");
        }
        if (hasVpnSignal(event)) {
            reasons.add("vpn_usage");
        }

        return new ArrayList<>(reasons);
    }

    private AgentFinalizePayload copyPayload(AgentFinalizePayload source) {
        return AgentFinalizePayload.builder()
                .riskScore(source.getRiskScore())
                .category(source.getCategory())
                .summary(source.getSummary())
                .reasons(source.getReasons() == null ? List.of() : new ArrayList<>(source.getReasons()))
                .tags(source.getTags() == null ? List.of() : new ArrayList<>(source.getTags()))
                .recommendedAction(source.getRecommendedAction())
                .fallbackUsed(Boolean.TRUE.equals(source.getFallbackUsed()))
                .confidenceScore(source.getConfidenceScore())
                .confidenceLabel(source.getConfidenceLabel())
                .build();
    }

    private boolean isLogin(AuditEvent event) {
        return event != null
                && event.getEventType() != null
                && event.getEventType().toUpperCase().contains("LOGIN");
    }

    private boolean isFailed(AuditEvent event) {
        return event != null
                && event.getStatus() != null
                && (
                event.getStatus().equalsIgnoreCase("FAILED")
                        || event.getStatus().equalsIgnoreCase("FAILURE")
        );
    }

    private boolean isSuspicious(AuditEvent event) {
        if (!isLogin(event)) {
            return false;
        }

        boolean failed = isFailed(event);
        boolean geoAnomaly = hasBooleanMetadata(event, "geoAnomaly");
        boolean knownDevice = readBooleanMetadata(event, "knownDevice", true);
        boolean vpn = hasVpnSignal(event);

        return failed || geoAnomaly || !knownDevice || vpn;
    }

    private boolean isAdminAction(AuditEvent event) {
        if (event == null) {
            return false;
        }

        String action = safe(event.getAction()).toUpperCase();
        String eventType = safe(event.getEventType()).toUpperCase();

        return eventType.contains("ADMIN")
                || action.contains("ADMIN")
                || action.contains("PRIVILEGE")
                || action.contains("ROLE")
                || action.contains("POLICY")
                || action.contains("EXPORT");
    }

    private boolean hasBooleanMetadata(AuditEvent event, String key) {
        return readBooleanMetadata(event, key, false);
    }

    private boolean readBooleanMetadata(AuditEvent event, String key, boolean defaultValue) {
        if (event == null || event.getMetadata() == null) {
            return defaultValue;
        }

        Object value = event.getMetadata().get(key);
        if (value == null) {
            return defaultValue;
        }

        if (value instanceof Boolean b) {
            return b;
        }

        return Boolean.parseBoolean(String.valueOf(value));
    }

    private boolean hasVpnSignal(AuditEvent event) {
        if (event == null || event.getMetadata() == null) {
            return false;
        }

        String attemptSource = String.valueOf(event.getMetadata().getOrDefault("attemptSource", ""));
        String ipReputation = String.valueOf(event.getMetadata().getOrDefault("ipReputation", ""));
        String combined = (attemptSource + " " + ipReputation).toUpperCase();
        return combined.contains("VPN") || combined.contains("TOR") || combined.contains("PROXY");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
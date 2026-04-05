package com.agentic.ai.spring_ai_service.audit.orchestrator;

import com.agentic.ai.spring_ai_service.audit.dto.agent.AgentDecision;
import com.agentic.ai.spring_ai_service.audit.dto.agent.AgentFinalizePayload;
import com.agentic.ai.spring_ai_service.audit.dto.agent.AgentToolRequest;
import com.agentic.ai.spring_ai_service.audit.model.AuditEvent;
import com.agentic.ai.spring_ai_service.audit.model.MatchedPolicyEvidence;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class AgentDecisionService {

    public AgentDecision decide(
            Object auditEvent,
            List<MatchedPolicyEvidence> matchedPolicyEvidence,
            List<String> observations,
            int currentIteration,
            int maxIterations
    ) {
        AuditEvent event = (AuditEvent) auditEvent;

        boolean loginEvent = isLoginEvent(event);
        boolean adminAction = isAdminAction(event);
        boolean failed = isFailed(event);
        boolean grounded = matchedPolicyEvidence != null && !matchedPolicyEvidence.isEmpty();

        boolean geoAnomaly = hasBooleanMetadata(event, "geoAnomaly");
        boolean knownDevice = readBooleanMetadata(event, "knownDevice", true);
        boolean suspiciousLogin = loginEvent && (failed || geoAnomaly || !knownDevice || hasVpnSignal(event));

        boolean hasRecentEventsObservation = hasObservationForTool(observations, "recent");
        boolean hasFailedCountObservation = hasObservationForTool(observations, "failed login count");
        boolean hasActivitySummaryObservation = hasObservationForTool(observations, "activity summary");

        if (currentIteration >= maxIterations) {
            log.info("[DECISION] finalize currentIteration={} reason=max_iterations", currentIteration);
            return finalizeDecision(
                    "Reached bounded reasoning limit. Finalizing with available evidence.",
                    failed,
                    loginEvent,
                    adminAction,
                    grounded,
                    true,
                    suspiciousLogin
            );
        }

        if (adminAction) {
            if (!hasActivitySummaryObservation) {
                return toolDecision(
                        currentIteration,
                        "Need user activity summary to review unusual admin behavior.",
                        "getUserActivitySummary",
                        Map.of("actor", safe(event.getActor()))
                );
            }

            if (!hasRecentEventsObservation) {
                return toolDecision(
                        currentIteration,
                        "Need recent events to correlate the admin action with surrounding activity.",
                        "getRecentEvents",
                        Map.of("actor", safe(event.getActor()), "limit", 5)
                );
            }

            return finalizeDecision(
                    "Enough admin context collected to finalize.",
                    failed,
                    loginEvent,
                    true,
                    grounded,
                    false,
                    suspiciousLogin
            );
        }

        if (suspiciousLogin) {
            if (!hasRecentEventsObservation) {
                return toolDecision(
                        currentIteration,
                        "Need recent event history for suspicious login sequence analysis.",
                        "getRecentEvents",
                        Map.of("actor", safe(event.getActor()), "limit", 5)
                );
            }

            if ((failed || containsFailureSignal(observations)) && !hasFailedCountObservation) {
                return toolDecision(
                        currentIteration,
                        "Need failed login count to validate suspicious login pattern.",
                        "getFailedLoginCount",
                        Map.of("actor", safe(event.getActor()), "limit", 5)
                );
            }

            if ((geoAnomaly || !knownDevice || hasVpnSignal(event)) && !hasActivitySummaryObservation) {
                return toolDecision(
                        currentIteration,
                        "Need user activity summary to assess geo/device anomaly for this login.",
                        "getUserActivitySummary",
                        Map.of("actor", safe(event.getActor()))
                );
            }

            return finalizeDecision(
                    "Enough suspicious login evidence collected to finalize.",
                    failed,
                    loginEvent,
                    adminAction,
                    grounded,
                    false,
                    true
            );
        }

        if (loginEvent) {
            return finalizeDecision(
                    "Successful login with no suspicious signals.",
                    false,
                    true,
                    false,
                    grounded,
                    false,
                    false
            );
        }

        return finalizeDecision(
                "Enough evidence collected to finalize.",
                failed,
                loginEvent,
                adminAction,
                grounded,
                false,
                false
        );
    }

    private AgentDecision toolDecision(
            int currentIteration,
            String thought,
            String toolName,
            Map<String, Object> args
    ) {
        log.info(
                "[DECISION] choose-tool currentIteration={} thought='{}' tool={}",
                currentIteration,
                thought,
                toolName
        );

        return AgentDecision.builder()
                .thought(thought)
                .action("TOOL")
                .decision("continue")
                .toolRequest(AgentToolRequest.builder()
                        .toolName(toolName)
                        .toolArgs(args)
                        .build())
                .build();
    }

    private AgentDecision finalizeDecision(
            String thought,
            boolean failed,
            boolean loginEvent,
            boolean adminAction,
            boolean grounded,
            boolean fallbackUsed,
            boolean suspiciousLogin
    ) {
        int riskScore;
        String category;
        String summary;
        List<String> reasons;
        List<String> tags;
        String recommendedAction;

        if (adminAction) {
            riskScore = 8;
            category = "UNUSUAL_ADMIN_ACTION";
            summary = "Admin activity requires review based on action type and collected evidence.";
            reasons = grounded
                    ? List.of("admin_action_detected", "policy_match", "tool_observation")
                    : List.of("admin_action_detected", "tool_observation");
            tags = grounded
                    ? List.of("admin", "review", "policy-grounded")
                    : List.of("admin", "review");
            recommendedAction = "Review the admin activity and confirm authorization.";
        } else if (suspiciousLogin) {
            riskScore = failed ? 6 : 5;
            category = "SUSPICIOUS_LOGIN";
            summary = "Login activity appears suspicious based on event context and collected evidence.";
            reasons = grounded
                    ? List.of("suspicious_login_detected", "policy_match", "tool_observation")
                    : List.of("suspicious_login_detected", "tool_observation");
            tags = grounded
                    ? List.of("login", "review", "policy-grounded")
                    : List.of("login", "review");
            recommendedAction = "Review the login activity and verify whether it was expected.";
        } else if (loginEvent) {
            riskScore = 2;
            category = "BENIGN_LOGIN";
            summary = "Successful login appears low risk based on available event context and collected history.";
            reasons = grounded
                    ? List.of("successful_login", "no_suspicious_signal_detected", "policy_context_reviewed")
                    : List.of("successful_login", "no_suspicious_signal_detected");
            tags = grounded
                    ? List.of("login", "low-risk", "policy-grounded")
                    : List.of("login", "low-risk");
            recommendedAction = "No immediate action required. Continue routine monitoring.";
        } else {
            riskScore = 4;
            category = "REVIEW_REQUIRED";
            summary = "Event was analyzed with available evidence and should be reviewed if needed.";
            reasons = grounded
                    ? List.of("event_reviewed", "policy_match", "tool_observation")
                    : List.of("event_reviewed", "tool_observation");
            tags = grounded
                    ? List.of("audit", "policy-grounded")
                    : List.of("audit");
            recommendedAction = "Review the event if it appears unusual in broader context.";
        }

        log.info(
                "[DECISION] finalize thought='{}' category={} riskScore={}",
                thought,
                category,
                riskScore
        );

        return AgentDecision.builder()
                .thought(thought)
                .action("FINALIZE")
                .decision("stop")
                .finalResponse(AgentFinalizePayload.builder()
                        .riskScore(riskScore)
                        .category(category)
                        .summary(summary)
                        .reasons(reasons)
                        .tags(tags)
                        .recommendedAction(recommendedAction)
                        .fallbackUsed(fallbackUsed)
                        .build())
                .build();
    }

    private boolean isLoginEvent(AuditEvent event) {
        return event != null
                && event.getEventType() != null
                && event.getEventType().toUpperCase().contains("LOGIN");
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

    private boolean isFailed(AuditEvent event) {
        if (event == null || event.getStatus() == null) {
            return false;
        }

        String status = event.getStatus().trim().toUpperCase();
        return "FAILURE".equals(status) || "FAILED".equals(status);
    }

    private boolean containsFailureSignal(List<String> observations) {
        if (observations == null || observations.isEmpty()) {
            return false;
        }

        return observations.stream()
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .anyMatch(obs ->
                        obs.contains("failed")
                                || obs.contains("failure")
                                || obs.contains("count"));
    }

    private boolean hasObservationForTool(List<String> observations, String marker) {
        if (observations == null || observations.isEmpty()) {
            return false;
        }

        String lowerMarker = marker.toLowerCase();
        return observations.stream()
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .anyMatch(obs -> obs.contains(lowerMarker));
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
}
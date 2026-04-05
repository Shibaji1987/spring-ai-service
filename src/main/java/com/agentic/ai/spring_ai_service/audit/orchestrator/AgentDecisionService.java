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
        int observationCount = observations != null ? observations.size() : 0;

        if (currentIteration >= maxIterations) {
            log.info("[DECISION] finalize currentIteration={} reason=max_iterations", currentIteration);
            return finalizeDecision(
                    "Reached bounded reasoning limit. Finalizing with available evidence.",
                    failed,
                    loginEvent,
                    adminAction,
                    grounded,
                    true
            );
        }

        if (observationCount == 0) {
            if (loginEvent) {
                log.info("[DECISION] choose-tool currentIteration={} thought='Need recent event history for login sequence analysis.' tool=getRecentEvents", currentIteration);
                return AgentDecision.builder()
                        .thought("Need recent event history for login sequence analysis.")
                        .action("TOOL")
                        .decision("continue")
                        .toolRequest(
                                AgentToolRequest.builder()
                                        .toolName("getRecentEvents")
                                        .toolArgs(Map.of(
                                                "actor", safe(event.getActor()),
                                                "limit", 5
                                        ))
                                        .build()
                        )
                        .build();
            }

            if (adminAction) {
                log.info("[DECISION] choose-tool currentIteration={} thought='Need user activity summary for admin action review.' tool=getUserActivitySummary", currentIteration);
                return AgentDecision.builder()
                        .thought("Need user activity summary for admin action review.")
                        .action("TOOL")
                        .decision("continue")
                        .toolRequest(
                                AgentToolRequest.builder()
                                        .toolName("getUserActivitySummary")
                                        .toolArgs(Map.of(
                                                "actor", safe(event.getActor())
                                        ))
                                        .build()
                        )
                        .build();
            }
        }

        if (observationCount == 1 && loginEvent && (failed || containsFailureSignal(observations))) {
            log.info("[DECISION] choose-tool currentIteration={} thought='Need failed login count to validate suspicious login pattern.' tool=getFailedLoginCount", currentIteration);
            return AgentDecision.builder()
                    .thought("Need failed login count to validate suspicious login pattern.")
                    .action("TOOL")
                    .decision("continue")
                    .toolRequest(
                            AgentToolRequest.builder()
                                    .toolName("getFailedLoginCount")
                                    .toolArgs(Map.of(
                                            "actor", safe(event.getActor())
                                    ))
                                    .build()
                    )
                    .build();
        }

        log.info("[DECISION] finalize currentIteration={} observations={} thought='Enough evidence collected to finalize.'",
                currentIteration, observationCount);

        return finalizeDecision(
                "Enough evidence collected to finalize.",
                failed,
                loginEvent,
                adminAction,
                grounded,
                false
        );
    }

    private AgentDecision finalizeDecision(
            String thought,
            boolean failed,
            boolean loginEvent,
            boolean adminAction,
            boolean grounded,
            boolean fallbackUsed
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
        } else if (failed) {
            riskScore = 7;
            category = "SUSPICIOUS_LOGIN";
            summary = "Login activity appears suspicious based on event status and collected evidence.";
            reasons = grounded
                    ? List.of("failed_login_detected", "policy_match", "tool_observation")
                    : List.of("failed_login_detected", "tool_observation");
            tags = grounded
                    ? List.of("login", "review", "policy-grounded")
                    : List.of("login", "review");
            recommendedAction = "Review the login attempt and verify whether it was expected.";
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

        return AgentDecision.builder()
                .thought(thought)
                .action("FINALIZE")
                .decision("stop")
                .finalResponse(
                        AgentFinalizePayload.builder()
                                .riskScore(riskScore)
                                .category(category)
                                .summary(summary)
                                .reasons(reasons)
                                .tags(tags)
                                .recommendedAction(recommendedAction)
                                .fallbackUsed(fallbackUsed)
                                .build()
                )
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
        return event != null
                && event.getStatus() != null
                && event.getStatus().equalsIgnoreCase("FAILURE");
    }

    private boolean containsFailureSignal(List<String> observations) {
        if (observations == null || observations.isEmpty()) {
            return false;
        }
        return observations.stream().anyMatch(obs ->
                obs != null && (
                        obs.toLowerCase().contains("failed")
                                || obs.toLowerCase().contains("failure")
                                || obs.toLowerCase().contains("count")
                )
        );
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
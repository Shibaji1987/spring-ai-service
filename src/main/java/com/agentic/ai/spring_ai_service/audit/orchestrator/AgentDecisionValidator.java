package com.agentic.ai.spring_ai_service.audit.orchestrator;

import com.agentic.ai.spring_ai_service.audit.dto.agent.AgentDecision;
import com.agentic.ai.spring_ai_service.audit.dto.agent.AgentToolRequest;
import com.agentic.ai.spring_ai_service.audit.model.AuditEvent;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class AgentDecisionValidator {

    private static final Set<String> ALLOWED_TOOLS = Set.of(
            "getUserActivitySummary",
            "getFailedLoginCount",
            "getRecentEvents"
    );

    public AgentDecision validate(
            AgentDecision decision,
            AuditEvent auditEvent,
            int iteration,
            int maxIterations
    ) {
        if (decision == null) {
            return null;
        }

        decision.setThought(sanitizeRationale(decision.getThought()));

        if (iteration >= maxIterations || "FINALIZE".equalsIgnoreCase(decision.getAction())) {
            return validateFinalDecision(decision);
        }

        if (!"TOOL".equalsIgnoreCase(decision.getAction())) {
            throw new IllegalArgumentException("LLM decision action must be TOOL or FINALIZE.");
        }

        AgentToolRequest request = decision.getToolRequest();
        if (request == null || !ALLOWED_TOOLS.contains(request.getToolName())) {
            throw new IllegalArgumentException("LLM requested a tool that is not allowlisted.");
        }

        Map<String, Object> safeArgs = new HashMap<>();
        safeArgs.put("actor", auditEvent == null || auditEvent.getActor() == null ? "" : auditEvent.getActor());

        if ("getRecentEvents".equals(request.getToolName())) {
            safeArgs.put("limit", clampLimit(request.getToolArgs()));
        }

        request.setToolArgs(safeArgs);
        decision.setAction("TOOL");
        decision.setDecision("continue");
        decision.setFinalResponse(null);
        return decision;
    }

    private AgentDecision validateFinalDecision(AgentDecision decision) {
        if (decision.getFinalResponse() == null) {
            throw new IllegalArgumentException("FINALIZE decision must include finalResponse.");
        }

        decision.setAction("FINALIZE");
        decision.setDecision("stop");
        decision.setToolRequest(null);
        return decision;
    }

    private int clampLimit(Map<String, Object> args) {
        if (args == null || args.get("limit") == null) {
            return 5;
        }

        try {
            int requested = Integer.parseInt(String.valueOf(args.get("limit")));
            return Math.max(1, Math.min(requested, 10));
        } catch (NumberFormatException ex) {
            return 5;
        }
    }

    private String sanitizeRationale(String rationale) {
        if (rationale == null || rationale.isBlank()) {
            return "The model selected the next bounded investigation action.";
        }

        String singleLine = rationale.replaceAll("\\s+", " ").trim();
        return singleLine.length() <= 240 ? singleLine : singleLine.substring(0, 240);
    }
}

package com.agentic.ai.spring_ai_service.audit.tools;

import com.agentic.ai.spring_ai_service.audit.model.AuditEvent;
import com.agentic.ai.spring_ai_service.audit.model.ToolExecutionRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvestigationToolGateway {

    private final AuditTools auditTools;
    private final InvestigationEvidenceService evidenceService;
    private final ObjectMapper objectMapper;

    public ToolExecutionRecord executeWhitelisted(
            String toolName,
            AuditEvent event,
            Map<String, Object> input
    ) {
        if (toolName == null || toolName.isBlank()) {
            return rejected("UNKNOWN", input, "Tool name is missing.");
        }
        if (!InvestigationToolCatalog.contains(toolName)) {
            return rejected(toolName, input, "Tool is not whitelisted: " + toolName);
        }
        if (event == null && requiresEventContext(toolName)) {
            return rejected(toolName, input, "Trusted audit-event context is required for this tool.");
        }

        return switch (toolName) {
            case "getUserActivitySummary" -> executeLegacy(
                    toolName, input, "audit_events.actor_history", 0.90,
                    () -> auditTools.getUserActivitySummary(stringArg(input, "actor")));
            case "getFailedLoginCount" -> executeLegacy(
                    toolName, input, "audit_events.authentication_history", 0.95,
                    () -> auditTools.getFailedLoginCount(stringArg(input, "actor")));
            case "getRecentEvents" -> executeLegacy(
                    toolName, input, "audit_events.actor_history", 0.90,
                    () -> auditTools.getRecentEvents(stringArg(input, "actor"), intArg(input, "limit", 5)));
            case "getIdentityRiskProfile" -> executeEvidence(toolName, input, () -> evidenceService.identityRisk(event));
            case "getAuthenticationRisk" -> executeEvidence(toolName, input, () -> evidenceService.authenticationRisk(event));
            case "getBehavioralBaseline" -> executeEvidence(toolName, input, () -> evidenceService.behavioralBaseline(event));
            case "getRelatedEventSequence" -> executeEvidence(
                    toolName, input, () -> evidenceService.relatedEventSequence(event, intArg(input, "hours", 24)));
            case "getAssetRiskProfile" -> executeEvidence(toolName, input, () -> evidenceService.assetRisk(event));
            case "getNetworkRiskContext" -> executeEvidence(toolName, input, () -> evidenceService.networkRisk(event));
            case "getSessionRiskContext" -> executeEvidence(toolName, input, () -> evidenceService.sessionRisk(event));
            case "getAuthorizationContext" -> executeEvidence(toolName, input, () -> evidenceService.authorizationContext(event));
            case "getDataExposureContext" -> executeEvidence(toolName, input, () -> evidenceService.dataExposure(event));
            case "searchApplicablePolicies" -> executeEvidence(toolName, input, () -> evidenceService.applicablePolicies(event));
            case "getControlCoverage" -> executeEvidence(toolName, input, () -> evidenceService.controlCoverage(event));
            case "getThreatIndicatorContext" -> executeEvidence(toolName, input, () -> evidenceService.threatIndicators(event));
            default -> rejected(toolName, input, "Unsupported tool: " + toolName);
        };
    }

    public ToolExecutionRecord executeWhitelisted(String toolName, Map<String, Object> input) {
        return executeWhitelisted(toolName, null, input);
    }

    public ToolExecutionRecord execute(String toolName, String inputSummary, Supplier<Object> supplier) {
        long start = System.currentTimeMillis();
        try {
            Object result = supplier.get();
            return success(toolName, inputSummary, String.valueOf(result), start, null, null, List.of(), List.of());
        } catch (Exception ex) {
            return failure(toolName, inputSummary, start, ex);
        }
    }

    public ToolExecutionRecord execute(String toolName, Map<String, Object> input, Supplier<Object> supplier) {
        return execute(toolName, summarizeInput(input), supplier);
    }

    private ToolExecutionRecord executeEvidence(
            String toolName,
            Map<String, Object> input,
            Supplier<InvestigationEvidence> supplier
    ) {
        long start = System.currentTimeMillis();
        try {
            InvestigationEvidence evidence = supplier.get();
            return success(
                    toolName,
                    summarizeInput(input),
                    toJson(evidence),
                    start,
                    evidence.source(),
                    evidence.confidence(),
                    evidence.evidenceIds(),
                    evidence.limitations());
        } catch (Exception ex) {
            return failure(toolName, summarizeInput(input), start, ex);
        }
    }

    private ToolExecutionRecord executeLegacy(
            String toolName,
            Map<String, Object> input,
            String source,
            double confidence,
            Supplier<Object> supplier
    ) {
        long start = System.currentTimeMillis();
        try {
            Object result = supplier.get();
            return success(
                    toolName, summarizeInput(input), String.valueOf(result), start,
                    source, confidence, List.of(), List.of());
        } catch (Exception ex) {
            return failure(toolName, summarizeInput(input), start, ex);
        }
    }

    private ToolExecutionRecord success(
            String toolName,
            String inputSummary,
            String outputSummary,
            long start,
            String source,
            Double confidence,
            List<String> evidenceIds,
            List<String> limitations
    ) {
        long duration = System.currentTimeMillis() - start;
        log.info("Tool executed successfully. tool={} durationMs={} source={}", toolName, duration, source);
        return ToolExecutionRecord.builder()
                .toolName(toolName)
                .success(true)
                .durationMs(duration)
                .inputSummary(inputSummary)
                .outputSummary(outputSummary)
                .executedAt(LocalDateTime.now())
                .source(source)
                .confidence(confidence)
                .evidenceIds(evidenceIds)
                .limitations(limitations)
                .build();
    }

    private ToolExecutionRecord failure(String toolName, String inputSummary, long start, Exception ex) {
        long duration = System.currentTimeMillis() - start;
        log.warn("Tool execution failed. tool={} durationMs={} error={}", toolName, duration, ex.getMessage());
        return ToolExecutionRecord.builder()
                .toolName(toolName)
                .success(false)
                .durationMs(duration)
                .inputSummary(inputSummary)
                .errorMessage(ex.getMessage())
                .executedAt(LocalDateTime.now())
                .limitations(List.of("The evidence source could not be queried."))
                .build();
    }

    private ToolExecutionRecord rejected(String toolName, Map<String, Object> input, String error) {
        return ToolExecutionRecord.builder()
                .toolName(toolName)
                .success(false)
                .durationMs(0L)
                .inputSummary(summarizeInput(input))
                .errorMessage(error)
                .executedAt(LocalDateTime.now())
                .limitations(List.of(error))
                .build();
    }

    private boolean requiresEventContext(String toolName) {
        return !List.of("getUserActivitySummary", "getFailedLoginCount", "getRecentEvents").contains(toolName);
    }

    private String stringArg(Map<String, Object> input, String key) {
        if (input == null || input.get(key) == null) {
            return "";
        }
        return String.valueOf(input.get(key));
    }

    private int intArg(Map<String, Object> input, String key, int defaultValue) {
        if (input == null || input.get(key) == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(input.get(key)));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private String summarizeInput(Map<String, Object> input) {
        return input == null ? "{}" : input.toString();
    }

    private String toJson(InvestigationEvidence evidence) {
        try {
            return objectMapper.writeValueAsString(evidence);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize investigation evidence.", ex);
        }
    }
}

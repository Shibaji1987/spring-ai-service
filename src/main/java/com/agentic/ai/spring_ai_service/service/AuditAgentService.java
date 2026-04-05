package com.agentic.ai.spring_ai_service.service;

import com.agentic.ai.spring_ai_service.audit.dto.agent.AgentFinalizePayload;
import com.agentic.ai.spring_ai_service.audit.dto.response.AuditAnalysisResponseDto;
import com.agentic.ai.spring_ai_service.audit.mapper.AuditAnalysisMapper;
import com.agentic.ai.spring_ai_service.audit.model.AnalysisDiagnostics;
import com.agentic.ai.spring_ai_service.audit.model.AuditAiAnalysis;
import com.agentic.ai.spring_ai_service.audit.model.AuditEvent;
import com.agentic.ai.spring_ai_service.audit.model.KnowledgeChunk;
import com.agentic.ai.spring_ai_service.audit.model.MatchedPolicyEvidence;
import com.agentic.ai.spring_ai_service.audit.model.ToolExecutionRecord;
import com.agentic.ai.spring_ai_service.audit.orchestrator.BoundedReActOrchestrator;
import com.agentic.ai.spring_ai_service.audit.orchestrator.ReActExecutionResult;
import com.agentic.ai.spring_ai_service.audit.tools.AuditTools;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditAgentService {

    private static final Logger log = LoggerFactory.getLogger(AuditAgentService.class);

    private static final int MAX_TOOL_CALLS = 3;
    private static final int TOP_K_POLICY_CHUNKS = 3;

    private final ChatClient.Builder chatClientBuilder;
    private final AuditEventService auditEventService;
    private final AuditTools auditTools;
    private final KnowledgeRetrievalService knowledgeRetrievalService;

    private final AuditAnalysisPersistenceService auditAnalysisPersistenceService;
    private final AnalysisConfidenceService analysisConfidenceService;
    private final AnalysisResponseValidator analysisResponseValidator;
    private final AuditAnalysisMapper auditAnalysisMapper;
    private final BoundedReActOrchestrator boundedReActOrchestrator;

    /**
     * Base deterministic analysis without tool execution.
     */
    public AuditAnalysisResponseDto analyzeEvent(String eventId) {
        AuditEvent auditEvent = auditEventService.getEventById(eventId);
        String eventText = buildEventText(auditEvent);

        List<MatchedPolicyEvidence> matchedPolicyEvidence = retrievePolicyEvidence(eventText);
        boolean grounded = !matchedPolicyEvidence.isEmpty();

        AgentFinalizePayload finalPayload = generateDeterministicPayload(auditEvent, matchedPolicyEvidence);
        finalPayload = analysisResponseValidator.normalize(finalPayload);

        double confidenceScore = analysisConfidenceService.computeConfidenceScore(
                matchedPolicyEvidence.size(),
                0,
                Boolean.TRUE.equals(finalPayload.getFallbackUsed()),
                grounded,
                safeRiskScore(finalPayload)
        );
        finalPayload.setConfidenceScore(confidenceScore);
        finalPayload.setConfidenceLabel(analysisConfidenceService.toConfidenceLabel(confidenceScore));

        AnalysisDiagnostics diagnostics = AnalysisDiagnostics.builder()
                .retrievedChunkCount(matchedPolicyEvidence.size())
                .evidenceUsedCount(matchedPolicyEvidence.size())
                .toolsRequestedCount(0)
                .toolsExecutedCount(0)
                .reasoningIterations(0)
                .maxReasoningIterations(0)
                .reasoningTruncated(false)
                .orchestrationMode("deterministic")
                .fallbackReason(Boolean.TRUE.equals(finalPayload.getFallbackUsed()) ? "deterministic_fallback" : null)
                .validatorStatus("OK")
                .scoringNotes("Confidence derived from retrieval grounding and deterministic scoring.")
                .build();

        AuditAiAnalysis saved = auditAnalysisPersistenceService.upsertAnalysis(
                eventId,
                finalPayload,
                matchedPolicyEvidence,
                List.of(),
                List.of(),
                diagnostics,
                grounded,
                Boolean.TRUE.equals(finalPayload.getFallbackUsed()),
                false,
                true,
                "deterministic-engine",
                "v3.0.0"
        );

        return auditAnalysisMapper.toDto(saved);
    }

    /**
     * Deterministic + tool-assisted analysis.
     */
    public AuditAnalysisResponseDto analyzeEventWithTools(String eventId) {
        AuditEvent auditEvent = auditEventService.getEventById(eventId);
        String actor = safe(auditEvent.getActor());
        String eventText = buildEventText(auditEvent);

        List<MatchedPolicyEvidence> matchedPolicyEvidence = retrievePolicyEvidence(eventText);
        List<ToolExecutionRecord> toolExecutions = new ArrayList<>();
        List<String> toolObservations = new ArrayList<>();

        safeToolCall(
                toolExecutions,
                "getUserActivitySummary",
                "actor=" + actor,
                () -> auditTools.getUserActivitySummary(actor)
        ).ifPresent(toolObservations::add);

        if (toolExecutions.size() < MAX_TOOL_CALLS && isLoginEvent(auditEvent)) {
            safeToolCall(
                    toolExecutions,
                    "getFailedLoginCount",
                    "actor=" + actor,
                    () -> auditTools.getFailedLoginCount(actor)
            ).ifPresent(toolObservations::add);
        }

        if (toolExecutions.size() < MAX_TOOL_CALLS && needsRecentSequence(auditEvent)) {
            safeToolCall(
                    toolExecutions,
                    "getRecentEvents",
                    "actor=" + actor + ", limit=5",
                    () -> auditTools.getRecentEvents(actor, 5)
            ).ifPresent(toolObservations::add);
        }

        AgentFinalizePayload finalPayload = generateToolAwarePayload(auditEvent, matchedPolicyEvidence, toolObservations);
        finalPayload = analysisResponseValidator.normalize(finalPayload);

        int successfulToolCount = (int) toolExecutions.stream()
                .filter(t -> Boolean.TRUE.equals(t.getSuccess()))
                .count();

        boolean grounded = !matchedPolicyEvidence.isEmpty();
        boolean fallbackUsed = Boolean.TRUE.equals(finalPayload.getFallbackUsed());

        double confidenceScore = analysisConfidenceService.computeConfidenceScore(
                matchedPolicyEvidence.size(),
                successfulToolCount,
                fallbackUsed,
                grounded,
                safeRiskScore(finalPayload)
        );
        finalPayload.setConfidenceScore(confidenceScore);
        finalPayload.setConfidenceLabel(analysisConfidenceService.toConfidenceLabel(confidenceScore));

        AnalysisDiagnostics diagnostics = AnalysisDiagnostics.builder()
                .retrievedChunkCount(matchedPolicyEvidence.size())
                .evidenceUsedCount(matchedPolicyEvidence.size())
                .toolsRequestedCount(toolExecutions.size())
                .toolsExecutedCount(toolExecutions.size())
                .reasoningIterations(0)
                .maxReasoningIterations(0)
                .reasoningTruncated(false)
                .orchestrationMode("llm_tools")
                .fallbackReason(fallbackUsed ? "tool_flow_fallback" : null)
                .validatorStatus("OK")
                .scoringNotes("Confidence derived from evidence count, successful tools, grounding, fallback, and risk score.")
                .build();

        AuditAiAnalysis saved = auditAnalysisPersistenceService.upsertAnalysis(
                eventId,
                finalPayload,
                matchedPolicyEvidence,
                toolExecutions,
                List.of(),
                diagnostics,
                grounded,
                fallbackUsed,
                !toolExecutions.isEmpty(),
                true,
                "openai-via-spring-ai",
                "v3.0.0"
        );

        return auditAnalysisMapper.toDto(saved);
    }

    /**
     * Bounded ReAct analysis with persisted reasoning trace.
     */
    public AuditAnalysisResponseDto analyzeEventWithLlmTools(String eventId) {
        AuditEvent auditEvent = auditEventService.getEventById(eventId);
        String eventText = buildEventText(auditEvent);

        List<MatchedPolicyEvidence> matchedPolicyEvidence = retrievePolicyEvidence(eventText);

        ReActExecutionResult result = boundedReActOrchestrator.execute(
                eventId,
                auditEvent,
                matchedPolicyEvidence
        );

        AuditAiAnalysis saved = auditAnalysisPersistenceService.upsertAnalysis(
                eventId,
                result.getFinalPayload(),
                result.getMatchedPolicyEvidence(),
                result.getToolExecutions(),
                result.getReasoningTrace(),
                result.getDiagnostics(),
                result.isGrounded(),
                result.isFallbackUsed(),
                result.isToolsInvoked(),
                result.isAnalysisSucceeded(),
                "openai-via-spring-ai",
                "v3.0.0"
        );

        return auditAnalysisMapper.toDto(saved);
    }

    /**
     * Optional compatibility method if your controller still calls the old name.
     */
    public AuditAnalysisResponseDto analyzeEventWithLlmDrivenTools(String eventId) {
        return analyzeEventWithLlmTools(eventId);
    }

    private List<MatchedPolicyEvidence> retrievePolicyEvidence(String eventText) {
        try {
            List<KnowledgeChunk> retrievedChunks = knowledgeRetrievalService.findTopKRelevantChunks(eventText, TOP_K_POLICY_CHUNKS);

            if (retrievedChunks == null || retrievedChunks.isEmpty()) {
                return List.of();
            }

            return retrievedChunks.stream()
                    .map(chunk -> MatchedPolicyEvidence.builder()
                            .policyName(safe(chunk.getDocumentTitle()))
                            .excerpt(safe(trimToLength(chunk.getText(), 500)))
                            .relevanceScore(null)
                            .sourceChunkId(chunk.getId())
                            .build())
                    .collect(Collectors.toList());

        } catch (Exception ex) {
            log.warn("Policy retrieval failed for analysis. error={}", ex.getMessage());
            return List.of();
        }
    }

    private AgentFinalizePayload generateDeterministicPayload(
            AuditEvent event,
            List<MatchedPolicyEvidence> matchedPolicyEvidence
    ) {
        boolean suspicious = looksSuspicious(event);
        boolean loginEvent = isLoginEvent(event);
        boolean adminAction = looksAdminAction(event);
        boolean grounded = matchedPolicyEvidence != null && !matchedPolicyEvidence.isEmpty();

        int riskScore = suspicious ? 7 : 2;
        String category = suspicious ? "suspicious_login" : "benign";

        if (adminAction) {
            category = "admin_action";
            riskScore = suspicious ? 8 : 5;
        } else if (loginEvent && suspicious) {
            category = "suspicious_login";
            riskScore = 7;
        }

        List<String> reasons = new ArrayList<>();
        if (loginEvent) reasons.add("login_event_detected");
        if (adminAction) reasons.add("admin_action_detected");
        if (suspicious) reasons.add("event_pattern_flagged");
        if (grounded) reasons.add("policy_evidence_matched");
        if (reasons.isEmpty()) reasons.add("low_risk_signal");

        List<String> tags = new ArrayList<>();
        tags.add(loginEvent ? "login" : "audit");
        if (adminAction) tags.add("admin");
        if (grounded) tags.add("policy_grounded");
        if (suspicious) tags.add("review");

        String summary = suspicious
                ? "Event shows potentially risky characteristics and should be reviewed."
                : "Event appears low risk based on available evidence.";

        String recommendedAction = suspicious
                ? "Review the event and verify whether the action was expected."
                : "No immediate action required, but retain the record for monitoring.";

        return AgentFinalizePayload.builder()
                .riskScore(riskScore)
                .category(category)
                .summary(summary)
                .reasons(reasons)
                .tags(tags)
                .recommendedAction(recommendedAction)
                .fallbackUsed(false)
                .build();
    }

    private AgentFinalizePayload generateToolAwarePayload(
            AuditEvent event,
            List<MatchedPolicyEvidence> matchedPolicyEvidence,
            List<String> toolObservations
    ) {
        boolean suspicious = looksSuspicious(event);
        boolean loginEvent = isLoginEvent(event);
        boolean adminAction = looksAdminAction(event);
        boolean grounded = matchedPolicyEvidence != null && !matchedPolicyEvidence.isEmpty();
        boolean hasToolEvidence = toolObservations != null && !toolObservations.isEmpty();

        int riskScore = suspicious ? 7 : 3;
        if (hasToolEvidence) {
            riskScore += 1;
        }
        if (adminAction) {
            riskScore = Math.max(riskScore, 8);
        }
        riskScore = Math.min(riskScore, 10);

        String category = "benign";
        if (adminAction) {
            category = "admin_action";
        } else if (loginEvent && suspicious) {
            category = "suspicious_login";
        } else if (suspicious) {
            category = "policy_violation";
        }

        List<String> reasons = new ArrayList<>();
        if (loginEvent) reasons.add("login_event_detected");
        if (adminAction) reasons.add("admin_action_detected");
        if (suspicious) reasons.add("suspicious_signal_present");
        if (grounded) reasons.add("policy_evidence_matched");
        if (hasToolEvidence) reasons.add("tool_context_collected");
        if (reasons.isEmpty()) reasons.add("low_risk_signal");

        List<String> tags = new ArrayList<>();
        tags.add(loginEvent ? "login" : "audit");
        if (adminAction) tags.add("admin");
        if (grounded) tags.add("policy_grounded");
        if (hasToolEvidence) tags.add("tool_grounded");
        if (suspicious) tags.add("review");

        String summary;
        if (adminAction) {
            summary = "Admin activity was analyzed with policy and tool context.";
        } else if (loginEvent && suspicious) {
            summary = "Login event appears suspicious after tool and policy review.";
        } else if (suspicious) {
            summary = "Event requires review based on retrieved evidence and tool context.";
        } else {
            summary = "Event appears low risk after policy and tool-based review.";
        }

        String recommendedAction = suspicious || adminAction
                ? "Review the event and confirm whether the activity was authorized."
                : "No immediate action required, but continue normal monitoring.";

        return AgentFinalizePayload.builder()
                .riskScore(riskScore)
                .category(category)
                .summary(summary)
                .reasons(reasons)
                .tags(tags)
                .recommendedAction(recommendedAction)
                .fallbackUsed(false)
                .build();
    }

    private java.util.Optional<String> safeToolCall(
            List<ToolExecutionRecord> toolExecutions,
            String toolName,
            String inputSummary,
            ToolSupplier supplier
    ) {
        long start = System.currentTimeMillis();

        try {
            Object data = supplier.get();
            long duration = System.currentTimeMillis() - start;

            ToolExecutionRecord record = ToolExecutionRecord.builder()
                    .toolName(toolName)
                    .success(true)
                    .durationMs(duration)
                    .inputSummary(inputSummary)
                    .outputSummary(data != null ? String.valueOf(data) : "null")
                    .errorMessage(null)
                    .executedAt(LocalDateTime.now())
                    .build();

            toolExecutions.add(record);

            log.info("Tool executed successfully. tool={} durationMs={}", toolName, duration);
            return java.util.Optional.of(record.getOutputSummary());

        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - start;

            ToolExecutionRecord record = ToolExecutionRecord.builder()
                    .toolName(toolName)
                    .success(false)
                    .durationMs(duration)
                    .inputSummary(inputSummary)
                    .outputSummary(null)
                    .errorMessage(ex.getMessage())
                    .executedAt(LocalDateTime.now())
                    .build();

            toolExecutions.add(record);

            log.warn("Tool execution failed. tool={} durationMs={} error={}", toolName, duration, ex.getMessage());
            return java.util.Optional.empty();
        }
    }

    private boolean isLoginEvent(AuditEvent event) {
        return event != null
                && event.getEventType() != null
                && event.getEventType().toUpperCase().contains("LOGIN");
    }

    private boolean looksSuspicious(AuditEvent event) {
        if (event == null) {
            return false;
        }

        if (event.getStatus() != null && event.getStatus().equalsIgnoreCase("FAILURE")) {
            return true;
        }

        if (event.getAction() != null) {
            String action = event.getAction().toUpperCase();
            return action.contains("PRIVILEGE")
                    || action.contains("DELETE")
                    || action.contains("EXPORT")
                    || action.contains("LOCK")
                    || action.contains("ADMIN");
        }

        return false;
    }

    private boolean looksAdminAction(AuditEvent event) {
        if (event == null) {
            return false;
        }

        String action = safe(event.getAction()).toUpperCase();
        return action.contains("ADMIN")
                || action.contains("PRIVILEGE")
                || action.contains("POLICY")
                || action.contains("ROLE")
                || action.contains("EXPORT");
    }

    private boolean needsRecentSequence(AuditEvent event) {
        return looksSuspicious(event) || isLoginEvent(event);
    }

    private String buildEventText(AuditEvent event) {
        if (event == null) {
            return "";
        }

        return """
                Event Type: %s
                Actor: %s
                Action: %s
                Target: %s
                Status: %s
                Event Time: %s
                Location: %s
                Device: %s
                Metadata: %s
                """.formatted(
                safe(event.getEventType()),
                safe(event.getActor()),
                safe(event.getAction()),
                safe(event.getTarget()),
                safe(event.getStatus()),
                event.getEventTime(),
                safe(extractLocation(event)),
                safe(extractDevice(event)),
                event.getMetadata()
        );
    }

    /**
     * These two helpers are defensive in case location/device are stored
     * either as direct fields or inside metadata in your current model.
     */
    private String extractLocation(AuditEvent event) {
        try {
            Object value = event.getClass().getMethod("getLocation").invoke(event);
            return value != null ? String.valueOf(value) : "";
        } catch (Exception ignored) {
            return extractMetadataValue(event, "location");
        }
    }

    private String extractDevice(AuditEvent event) {
        try {
            Object value = event.getClass().getMethod("getDevice").invoke(event);
            return value != null ? String.valueOf(value) : "";
        } catch (Exception ignored) {
            return extractMetadataValue(event, "device");
        }
    }

    @SuppressWarnings("unchecked")
    private String extractMetadataValue(AuditEvent event, String key) {
        try {
            Object metadata = event.getMetadata();
            if (metadata instanceof java.util.Map<?, ?> map) {
                Object value = map.get(key);
                return value != null ? String.valueOf(value) : "";
            }
        } catch (Exception ignored) {
            // no-op
        }
        return "";
    }

    private int safeRiskScore(AgentFinalizePayload payload) {
        return payload.getRiskScore() == null ? 0 : payload.getRiskScore();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String trimToLength(String value, int maxLen) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen);
    }

    @FunctionalInterface
    private interface ToolSupplier {
        Object get();
    }
}
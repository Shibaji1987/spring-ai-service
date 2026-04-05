package com.agentic.ai.spring_ai_service.audit.orchestrator;

import com.agentic.ai.spring_ai_service.audit.dto.agent.AgentDecision;
import com.agentic.ai.spring_ai_service.audit.dto.agent.AgentFinalizePayload;
import com.agentic.ai.spring_ai_service.audit.model.AnalysisDiagnostics;
import com.agentic.ai.spring_ai_service.audit.model.AuditEvent;
import com.agentic.ai.spring_ai_service.audit.model.MatchedPolicyEvidence;
import com.agentic.ai.spring_ai_service.audit.model.ReasoningStep;
import com.agentic.ai.spring_ai_service.audit.model.ToolExecutionRecord;
import com.agentic.ai.spring_ai_service.service.AnalysisConfidenceService;
import com.agentic.ai.spring_ai_service.service.AnalysisResponseValidator;
import com.agentic.ai.spring_ai_service.service.AuditAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoundedReActOrchestrator {

    private static final int MAX_ITERATIONS = 5;

    private final AgentDecisionService agentDecisionService;
    private final ToolExecutionOrchestrator toolExecutionOrchestrator;
    private final AuditAiService auditAiService;
    private final AnalysisConfidenceService confidenceService;
    private final AnalysisResponseValidator validator;

    public ReActExecutionResult execute(String eventId, Object auditEventObj, List<MatchedPolicyEvidence> matchedPolicyEvidence) {
        AuditEvent auditEvent = (AuditEvent) auditEventObj;

        List<ReasoningStep> reasoningTrace = new ArrayList<>();
        List<ToolExecutionRecord> toolExecutions = new ArrayList<>();
        List<String> observations = new ArrayList<>();
        Set<String> usedTools = new HashSet<>();

        AgentFinalizePayload finalPayload = null;
        boolean finalized = false;
        boolean fallbackUsed = false;

        log.info("[REACT][{}] started maxIterations={} matchedPolicyEvidence={}",
                eventId, MAX_ITERATIONS, matchedPolicyEvidence == null ? 0 : matchedPolicyEvidence.size());

        for (int i = 1; i <= MAX_ITERATIONS; i++) {
            log.info("[REACT][{}][step={}] iteration started observationsSoFar={}",
                    eventId, i, observations.size());

            AgentDecision decision = agentDecisionService.decide(
                    auditEvent,
                    matchedPolicyEvidence,
                    observations,
                    i,
                    MAX_ITERATIONS
            );

            ReasoningStep step = ReasoningStep.builder()
                    .stepNumber(i)
                    .thought(decision != null ? decision.getThought() : "No decision generated.")
                    .action(resolveAction(decision))
                    .decision(decision != null ? decision.getDecision() : "stop")
                    .timestamp(LocalDateTime.now())
                    .build();

            reasoningTrace.add(step);

            if (decision == null) {
                fallbackUsed = true;
                step.setObservation("Decision generation failed.");
                observations.add("Decision generation failed.");
                break;
            }

            log.info("[REACT][{}][step={}] decision thought='{}' action={} tool={} decision={}",
                    eventId,
                    i,
                    decision.getThought(),
                    decision.getAction(),
                    decision.getToolRequest() != null ? decision.getToolRequest().getToolName() : null,
                    decision.getDecision());

            if ("FINALIZE".equalsIgnoreCase(decision.getAction())) {
                step.setObservation("Finalization requested by LLM.");
                observations.add("Finalization requested by LLM.");
                finalized = true;
                break;
            }

            String toolName = decision.getToolRequest() != null ? decision.getToolRequest().getToolName() : null;

            if (toolName == null || toolName.isBlank()) {
                fallbackUsed = true;
                step.setObservation("Tool request was empty.");
                observations.add("Tool request was empty.");
                break;
            }

            if (usedTools.contains(toolName)) {
                step.setObservation("Skipped duplicate tool call for " + toolName + ".");
                observations.add("Skipped duplicate tool call for " + toolName + ".");
                log.info("[REACT][{}][step={}] duplicate tool skipped tool={}", eventId, i, toolName);
                finalized = true;
                break;
            }

            log.info("[REACT][{}][step={}] invoking tool={} args={}",
                    eventId,
                    i,
                    toolName,
                    decision.getToolRequest().getToolArgs());

            ToolExecutionRecord toolRecord = toolExecutionOrchestrator.execute(auditEvent, decision.getToolRequest());
            toolExecutions.add(toolRecord);
            usedTools.add(toolName);

            String observation = Boolean.TRUE.equals(toolRecord.getSuccess())
                    ? toolRecord.getOutputSummary()
                    : "Tool failed: " + toolRecord.getErrorMessage();

            step.setObservation(observation);
            observations.add(observation);

            log.info("[REACT][{}][step={}] toolResult success={} durationMs={} output='{}' error='{}'",
                    eventId,
                    i,
                    toolRecord.getSuccess(),
                    toolRecord.getDurationMs(),
                    toolRecord.getOutputSummary(),
                    toolRecord.getErrorMessage());
        }

        if (!finalized && observations.size() >= MAX_ITERATIONS) {
            fallbackUsed = true;
        }

        finalPayload = auditAiService.generateFinalAnalysis(
                auditEvent,
                matchedPolicyEvidence,
                observations,
                fallbackUsed
        );

        finalPayload = validator.normalize(finalPayload);

        int successfulToolCount = (int) toolExecutions.stream()
                .filter(t -> Boolean.TRUE.equals(t.getSuccess()))
                .count();

        boolean grounded = matchedPolicyEvidence != null && !matchedPolicyEvidence.isEmpty();

        double confidenceScore = confidenceService.computeConfidenceScore(
                grounded ? matchedPolicyEvidence.size() : 0,
                successfulToolCount,
                Boolean.TRUE.equals(finalPayload.getFallbackUsed()),
                grounded,
                finalPayload.getRiskScore() == null ? 0 : finalPayload.getRiskScore()
        );

        if ("BENIGN_LOGIN".equalsIgnoreCase(finalPayload.getCategory()) && confidenceScore > 0.75) {
            confidenceScore = 0.72;
        }

        finalPayload.setConfidenceScore(confidenceScore);
        finalPayload.setConfidenceLabel(confidenceService.toConfidenceLabel(confidenceScore));

        AnalysisDiagnostics diagnostics = AnalysisDiagnostics.builder()
                .retrievedChunkCount(grounded ? matchedPolicyEvidence.size() : 0)
                .evidenceUsedCount(grounded ? matchedPolicyEvidence.size() : 0)
                .toolsRequestedCount(toolExecutions.size())
                .toolsExecutedCount(toolExecutions.size())
                .reasoningIterations(reasoningTrace.size())
                .maxReasoningIterations(MAX_ITERATIONS)
                .reasoningTruncated(!finalized)
                .orchestrationMode("llm_react")
                .fallbackReason(Boolean.TRUE.equals(finalPayload.getFallbackUsed()) ? "bounded_react_fallback" : null)
                .validatorStatus("OK")
                .scoringNotes("Confidence derived from evidence count, tool success count, grounding, fallback, and risk score.")
                .build();

        log.info("[REACT][{}] completed grounded={} toolsInvoked={} iterations={} confidenceScore={} confidenceLabel={} riskScore={} category={}",
                eventId,
                grounded,
                !toolExecutions.isEmpty(),
                reasoningTrace.size(),
                finalPayload.getConfidenceScore(),
                finalPayload.getConfidenceLabel(),
                finalPayload.getRiskScore(),
                finalPayload.getCategory());

        return ReActExecutionResult.builder()
                .eventId(eventId)
                .finalPayload(finalPayload)
                .matchedPolicyEvidence(matchedPolicyEvidence == null ? List.of() : matchedPolicyEvidence)
                .toolExecutions(toolExecutions)
                .reasoningTrace(reasoningTrace)
                .diagnostics(diagnostics)
                .grounded(grounded)
                .fallbackUsed(Boolean.TRUE.equals(finalPayload.getFallbackUsed()))
                .toolsInvoked(!toolExecutions.isEmpty())
                .analysisSucceeded(true)
                .build();
    }

    private String resolveAction(AgentDecision decision) {
        if (decision == null) {
            return "UNKNOWN";
        }
        if ("FINALIZE".equalsIgnoreCase(decision.getAction())) {
            return "FINALIZE";
        }
        if (decision.getToolRequest() != null && decision.getToolRequest().getToolName() != null) {
            return "TOOL:" + decision.getToolRequest().getToolName();
        }
        return decision.getAction();
    }
}
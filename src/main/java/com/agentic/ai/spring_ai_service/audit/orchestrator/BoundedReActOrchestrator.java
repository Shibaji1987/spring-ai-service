package com.agentic.ai.spring_ai_service.audit.orchestrator;

import com.agentic.ai.spring_ai_service.audit.dto.agent.AgentDecision;
import com.agentic.ai.spring_ai_service.audit.dto.agent.AgentFinalizePayload;
import com.agentic.ai.spring_ai_service.audit.model.*;
import com.agentic.ai.spring_ai_service.service.AnalysisConfidenceService;
import com.agentic.ai.spring_ai_service.service.AnalysisResponseValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BoundedReActOrchestrator {

    private static final int MAX_ITERATIONS = 5;

    private final AgentDecisionService agentDecisionService;
    private final ToolExecutionOrchestrator toolExecutionOrchestrator;
    private final AnalysisConfidenceService confidenceService;
    private final AnalysisResponseValidator validator;

    public ReActExecutionResult execute(String eventId, Object auditEvent, List<MatchedPolicyEvidence> matchedPolicyEvidence) {
        List<ReasoningStep> reasoningTrace = new ArrayList<>();
        List<ToolExecutionRecord> toolExecutions = new ArrayList<>();
        List<String> observations = new ArrayList<>();

        AgentFinalizePayload finalPayload = null;
        boolean finalized = false;

        for (int i = 1; i <= MAX_ITERATIONS; i++) {
            AgentDecision decision = agentDecisionService.decide(auditEvent, matchedPolicyEvidence, observations, i, MAX_ITERATIONS);

            ReasoningStep step = ReasoningStep.builder()
                    .stepNumber(i)
                    .thought(decision.getThought())
                    .action(resolveAction(decision))
                    .decision(decision.getDecision())
                    .timestamp(LocalDateTime.now())
                    .build();

            reasoningTrace.add(step);

            if ("FINALIZE".equalsIgnoreCase(decision.getAction())) {
                finalPayload = validator.normalize(decision.getFinalResponse());
                step.setObservation("Finalization triggered.");
                finalized = true;
                break;
            }

            ToolExecutionRecord toolRecord = toolExecutionOrchestrator.execute(auditEvent, decision.getToolRequest());
            toolExecutions.add(toolRecord);

            String observation = Boolean.TRUE.equals(toolRecord.getSuccess())
                    ? toolRecord.getOutputSummary()
                    : "Tool failed: " + toolRecord.getErrorMessage();

            step.setObservation(observation);
            observations.add(observation);
        }

        if (!finalized) {
            finalPayload = validator.normalize(
                    AgentFinalizePayload.builder()
                            .riskScore(5)
                            .category("REVIEW_REQUIRED")
                            .summary("Bounded reasoning limit reached. Manual review recommended.")
                            .reasons(List.of("max_iterations_reached"))
                            .tags(List.of("fallback", "bounded-react"))
                            .recommendedAction("Review event manually.")
                            .fallbackUsed(true)
                            .build()
            );
        }

        int successfulToolCount = (int) toolExecutions.stream()
                .filter(t -> Boolean.TRUE.equals(t.getSuccess()))
                .count();

        boolean grounded = matchedPolicyEvidence != null && !matchedPolicyEvidence.isEmpty();
        boolean fallbackUsed = Boolean.TRUE.equals(finalPayload.getFallbackUsed());

        double confidenceScore = confidenceService.computeConfidenceScore(
                grounded ? matchedPolicyEvidence.size() : 0,
                successfulToolCount,
                fallbackUsed,
                grounded,
                finalPayload.getRiskScore() == null ? 0 : finalPayload.getRiskScore()
        );

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
                .fallbackReason(fallbackUsed ? "bounded_react_fallback" : null)
                .validatorStatus("OK")
                .scoringNotes("Confidence derived from evidence count, tool success count, grounding, fallback, and risk score.")
                .build();

        return ReActExecutionResult.builder()
                .eventId(eventId)
                .finalPayload(finalPayload)
                .matchedPolicyEvidence(matchedPolicyEvidence == null ? List.of() : matchedPolicyEvidence)
                .toolExecutions(toolExecutions)
                .reasoningTrace(reasoningTrace)
                .diagnostics(diagnostics)
                .grounded(grounded)
                .fallbackUsed(fallbackUsed)
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
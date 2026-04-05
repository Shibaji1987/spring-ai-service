package com.agentic.ai.spring_ai_service.audit.orchestrator;

import com.agentic.ai.spring_ai_service.audit.dto.agent.AgentDecision;
import com.agentic.ai.spring_ai_service.audit.dto.agent.AgentFinalizePayload;
import com.agentic.ai.spring_ai_service.audit.model.AnalysisDiagnostics;
import com.agentic.ai.spring_ai_service.audit.model.MatchedPolicyEvidence;
import com.agentic.ai.spring_ai_service.audit.model.ReasoningStep;
import com.agentic.ai.spring_ai_service.audit.model.ToolExecutionRecord;
import com.agentic.ai.spring_ai_service.service.AnalysisConfidenceService;
import com.agentic.ai.spring_ai_service.service.AnalysisResponseValidator;
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
    private final AnalysisConfidenceService confidenceService;
    private final AnalysisResponseValidator validator;

    public ReActExecutionResult execute(String eventId, Object auditEvent, List<MatchedPolicyEvidence> matchedPolicyEvidence) {
        List<ReasoningStep> reasoningTrace = new ArrayList<>();
        List<ToolExecutionRecord> toolExecutions = new ArrayList<>();
        List<String> observations = new ArrayList<>();
        Set<String> usedTools = new HashSet<>();

        AgentFinalizePayload finalPayload = null;
        boolean finalized = false;

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

            log.info("[REACT][{}][step={}] decision thought='{}' action={} tool={} decision={}",
                    eventId,
                    i,
                    decision != null ? decision.getThought() : null,
                    decision != null ? decision.getAction() : null,
                    decision != null && decision.getToolRequest() != null ? decision.getToolRequest().getToolName() : null,
                    decision != null ? decision.getDecision() : null);

            ReasoningStep step = ReasoningStep.builder()
                    .stepNumber(i)
                    .thought(decision != null ? decision.getThought() : "No decision generated.")
                    .action(resolveAction(decision))
                    .decision(decision != null ? decision.getDecision() : "stop")
                    .timestamp(LocalDateTime.now())
                    .build();

            reasoningTrace.add(step);

            if (decision == null) {
                finalPayload = validator.normalize(
                        AgentFinalizePayload.builder()
                                .riskScore(5)
                                .category("REVIEW_REQUIRED")
                                .summary("No decision generated. Manual review recommended.")
                                .reasons(List.of("decision_generation_failed"))
                                .tags(List.of("fallback", "decision-failure"))
                                .recommendedAction("Review event manually.")
                                .fallbackUsed(true)
                                .build()
                );
                step.setObservation("Decision generation failed.");
                finalized = true;
                break;
            }

            if ("FINALIZE".equalsIgnoreCase(decision.getAction())) {
                finalPayload = validator.normalize(decision.getFinalResponse());
                step.setObservation("Finalization triggered.");
                log.info("[REACT][{}][step={}] finalizing riskScore={} category={} fallbackUsed={}",
                        eventId,
                        i,
                        finalPayload != null ? finalPayload.getRiskScore() : null,
                        finalPayload != null ? finalPayload.getCategory() : null,
                        finalPayload != null ? finalPayload.getFallbackUsed() : null);
                finalized = true;
                break;
            }

            String toolName = decision.getToolRequest() != null ? decision.getToolRequest().getToolName() : null;
            if (toolName != null && usedTools.contains(toolName)) {
                step.setObservation("Skipped duplicate tool call for " + toolName + ".");
                observations.add(step.getObservation());

                log.info("[REACT][{}][step={}] duplicate tool skipped tool={}", eventId, i, toolName);

                finalPayload = validator.normalize(
                        AgentFinalizePayload.builder()
                                .riskScore(3)
                                .category("BENIGN_LOGIN")
                                .summary("Duplicate tool call avoided. Finalized using collected evidence.")
                                .reasons(List.of("duplicate_tool_prevented", "existing_evidence_used"))
                                .tags(List.of("bounded-react", "deduplicated"))
                                .recommendedAction("No immediate action required. Continue routine monitoring.")
                                .fallbackUsed(false)
                                .build()
                );
                finalized = true;
                break;
            }

            log.info("[REACT][{}][step={}] invoking tool={} args={}",
                    eventId,
                    i,
                    toolName,
                    decision.getToolRequest() != null ? decision.getToolRequest().getToolArgs() : null);

            ToolExecutionRecord toolRecord = toolExecutionOrchestrator.execute(auditEvent, decision.getToolRequest());
            toolExecutions.add(toolRecord);

            if (toolName != null) {
                usedTools.add(toolName);
            }

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
                .fallbackReason(fallbackUsed ? "bounded_react_fallback" : null)
                .validatorStatus("OK")
                .scoringNotes("Confidence derived from evidence count, tool success count, grounding, fallback, and risk score.")
                .build();

        log.info("[REACT][{}] completed grounded={} toolsInvoked={} iterations={} confidenceScore={} confidenceLabel={}",
                eventId,
                grounded,
                !toolExecutions.isEmpty(),
                reasoningTrace.size(),
                finalPayload.getConfidenceScore(),
                finalPayload.getConfidenceLabel());

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
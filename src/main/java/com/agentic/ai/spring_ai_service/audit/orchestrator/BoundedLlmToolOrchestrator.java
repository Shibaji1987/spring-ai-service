package com.agentic.ai.spring_ai_service.audit.orchestrator;

import com.agentic.ai.spring_ai_service.audit.dto.agent.AgentDecision;
import com.agentic.ai.spring_ai_service.audit.dto.agent.AgentFinalizePayload;
import com.agentic.ai.spring_ai_service.audit.dto.response.AuditAnalysisStreamEventDto;
import com.agentic.ai.spring_ai_service.audit.mapper.AuditAnalysisMapper;
import com.agentic.ai.spring_ai_service.audit.model.AnalysisDiagnostics;
import com.agentic.ai.spring_ai_service.audit.model.AuditEvent;
import com.agentic.ai.spring_ai_service.audit.model.MatchedPolicyEvidence;
import com.agentic.ai.spring_ai_service.audit.model.ReasoningStep;
import com.agentic.ai.spring_ai_service.audit.model.ToolExecutionRecord;
import com.agentic.ai.spring_ai_service.service.AnalysisConfidenceService;
import com.agentic.ai.spring_ai_service.service.AnalysisResponseValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoundedLlmToolOrchestrator {

    private static final int MAX_ITERATIONS = 5;
    private static final int MAX_TOOL_CALLS = 3;

    private final AgentDecisionService agentDecisionService;
    private final ToolExecutionOrchestrator toolExecutionOrchestrator;
    private final AnalysisConfidenceService confidenceService;
    private final AnalysisResponseValidator responseValidator;
    private final AuditAnalysisMapper analysisMapper;

    public ReActExecutionResult execute(
            String eventId,
            AuditEvent auditEvent,
            List<MatchedPolicyEvidence> policyEvidence
    ) {
        return execute(eventId, auditEvent, policyEvidence, null);
    }

    public ReActExecutionResult execute(
            String eventId,
            AuditEvent auditEvent,
            List<MatchedPolicyEvidence> policyEvidence,
            Consumer<AuditAnalysisStreamEventDto> progressSink
    ) {
        AgentRunState state = new AgentRunState();
        List<MatchedPolicyEvidence> safeEvidence = policyEvidence == null ? List.of() : policyEvidence;

        for (int iteration = 1; iteration <= MAX_ITERATIONS && !state.finished(); iteration++) {
            executeIteration(eventId, auditEvent, safeEvidence, iteration, state, progressSink);
        }

        return buildResult(eventId, safeEvidence, state);
    }

    private void executeIteration(
            String eventId,
            AuditEvent auditEvent,
            List<MatchedPolicyEvidence> policyEvidence,
            int iteration,
            AgentRunState state,
            Consumer<AuditAnalysisStreamEventDto> progressSink
    ) {
        emit(progressSink, eventId, "LLM_DECISION", "RUNNING",
                "LLM is evaluating whether additional evidence is required.", null);

        AgentDecision decision = agentDecisionService.decide(
                auditEvent,
                policyEvidence,
                state.observations(),
                iteration,
                MAX_ITERATIONS
        );

        if (decision == null) {
            state.fail("The LLM decision could not be generated.");
            emit(progressSink, eventId, "LLM_DECISION", "FAILED", state.failureReason(), null);
            return;
        }

        ReasoningStep step = createReasoningStep(iteration, decision);
        state.reasoningTrace().add(step);
        emit(progressSink, eventId, "LLM_DECISION", "COMPLETED", decision.getThought(), null);

        if ("FINALIZE".equalsIgnoreCase(decision.getAction())) {
            state.finalizeWith(decision.getFinalResponse());
            step.setObservation("The LLM finalized the assessment using available evidence.");
            emit(progressSink, eventId, "AI_REASONING", "COMPLETED",
                    "The LLM finalized the risk assessment.", null);
            return;
        }

        executeRequestedTool(eventId, auditEvent, decision, step, state, progressSink);
    }

    private void executeRequestedTool(
            String eventId,
            AuditEvent auditEvent,
            AgentDecision decision,
            ReasoningStep step,
            AgentRunState state,
            Consumer<AuditAnalysisStreamEventDto> progressSink
    ) {
        String toolName = decision.getToolRequest().getToolName();

        if (state.toolExecutions().size() >= MAX_TOOL_CALLS) {
            state.fail("The bounded tool-call limit was reached.");
            step.setObservation(state.failureReason());
            return;
        }

        if (!state.usedTools().add(toolName)) {
            state.fail("The LLM requested a duplicate tool: " + toolName);
            step.setObservation(state.failureReason());
            return;
        }

        emit(progressSink, eventId, "TOOL_REQUESTED", "COMPLETED",
                "LLM requested %s with %s.".formatted(toolName, decision.getToolRequest().getToolArgs()), null);
        emit(progressSink, eventId, "TOOL_EXECUTION", "RUNNING",
                "Executing allowlisted tool " + toolName + ".", null);

        ToolExecutionRecord record = toolExecutionOrchestrator.execute(auditEvent, decision.getToolRequest());
        state.recordToolResult(record);
        step.setObservation(state.observations().get(state.observations().size() - 1));

        emit(
                progressSink,
                eventId,
                "TOOL_EXECUTION",
                Boolean.TRUE.equals(record.getSuccess()) ? "COMPLETED" : "FAILED",
                toolResultMessage(record),
                record
        );
    }

    private ReActExecutionResult buildResult(
            String eventId,
            List<MatchedPolicyEvidence> policyEvidence,
            AgentRunState state
    ) {
        AgentFinalizePayload payload = responseValidator.normalize(
                state.finalPayload() == null ? fallbackPayload(state.failureReason()) : state.finalPayload()
        );
        boolean fallbackUsed = state.finalPayload() == null || Boolean.TRUE.equals(payload.getFallbackUsed());
        boolean grounded = !policyEvidence.isEmpty();
        int successfulTools = (int) state.toolExecutions().stream()
                .filter(tool -> Boolean.TRUE.equals(tool.getSuccess()))
                .count();

        double confidence = confidenceService.computeConfidenceScore(
                policyEvidence.size(),
                successfulTools,
                fallbackUsed,
                grounded,
                payload.getRiskScore()
        );
        payload.setConfidenceScore(confidence);
        payload.setConfidenceLabel(confidenceService.toConfidenceLabel(confidence));
        payload.setFallbackUsed(fallbackUsed);

        return ReActExecutionResult.builder()
                .eventId(eventId)
                .finalPayload(payload)
                .matchedPolicyEvidence(policyEvidence)
                .toolExecutions(state.toolExecutions())
                .reasoningTrace(state.reasoningTrace())
                .diagnostics(buildDiagnostics(policyEvidence, state, fallbackUsed))
                .grounded(grounded)
                .fallbackUsed(fallbackUsed)
                .toolsInvoked(!state.toolExecutions().isEmpty())
                .analysisSucceeded(state.finalPayload() != null)
                .build();
    }

    private AnalysisDiagnostics buildDiagnostics(
            List<MatchedPolicyEvidence> evidence,
            AgentRunState state,
            boolean fallbackUsed
    ) {
        return AnalysisDiagnostics.builder()
                .retrievedChunkCount(evidence.size())
                .evidenceUsedCount(evidence.size())
                .toolsRequestedCount(state.usedTools().size())
                .toolsExecutedCount(state.toolExecutions().size())
                .reasoningIterations(state.reasoningTrace().size())
                .maxReasoningIterations(MAX_ITERATIONS)
                .reasoningTruncated(state.finalPayload() == null)
                .orchestrationMode("llm_directed_tools")
                .fallbackReason(fallbackUsed ? defaultFailureReason(state.failureReason()) : null)
                .validatorStatus("OK")
                .scoringNotes("Confidence derives from grounding, successful tool observations, fallback state, and risk.")
                .build();
    }

    private ReasoningStep createReasoningStep(int iteration, AgentDecision decision) {
        String action = "FINALIZE".equalsIgnoreCase(decision.getAction())
                ? "FINALIZE"
                : "TOOL:" + decision.getToolRequest().getToolName();

        return ReasoningStep.builder()
                .stepNumber(iteration)
                .thought(decision.getThought())
                .action(action)
                .decision(decision.getDecision())
                .timestamp(LocalDateTime.now())
                .build();
    }

    private AgentFinalizePayload fallbackPayload(String reason) {
        return AgentFinalizePayload.builder()
                .riskScore(5)
                .category("REVIEW_REQUIRED")
                .summary("The bounded LLM investigation could not complete. Manual review is required.")
                .reasons(List.of(defaultFailureReason(reason)))
                .tags(List.of("fallback", "manual-review"))
                .recommendedAction("Review the event manually and inspect the recorded execution trace.")
                .fallbackUsed(true)
                .build();
    }

    private String toolResultMessage(ToolExecutionRecord record) {
        if (Boolean.TRUE.equals(record.getSuccess())) {
            return "%s completed: %s".formatted(record.getToolName(), record.getOutputSummary());
        }
        return "%s failed: %s".formatted(record.getToolName(), record.getErrorMessage());
    }

    private String defaultFailureReason(String reason) {
        return reason == null || reason.isBlank() ? "bounded_llm_investigation_incomplete" : reason;
    }

    private void emit(
            Consumer<AuditAnalysisStreamEventDto> sink,
            String eventId,
            String phase,
            String status,
            String message,
            ToolExecutionRecord toolExecution
    ) {
        if (sink == null) {
            return;
        }

        sink.accept(AuditAnalysisStreamEventDto.builder()
                .eventId(eventId)
                .phase(phase)
                .status(status)
                .message(message)
                .toolExecution(analysisMapper.toToolDto(toolExecution))
                .timestamp(Instant.now())
                .build());
    }

    private static final class AgentRunState {
        private final List<ReasoningStep> reasoningTrace = new ArrayList<>();
        private final List<ToolExecutionRecord> toolExecutions = new ArrayList<>();
        private final List<String> observations = new ArrayList<>();
        private final Set<String> usedTools = new HashSet<>();
        private AgentFinalizePayload finalPayload;
        private String failureReason;

        void recordToolResult(ToolExecutionRecord record) {
            toolExecutions.add(record);
            observations.add(Boolean.TRUE.equals(record.getSuccess())
                    ? "%s observation: %s".formatted(record.getToolName(), record.getOutputSummary())
                    : "%s failed: %s".formatted(record.getToolName(), record.getErrorMessage()));
        }

        void finalizeWith(AgentFinalizePayload payload) {
            this.finalPayload = payload;
        }

        void fail(String reason) {
            this.failureReason = reason;
        }

        boolean finished() {
            return finalPayload != null || failureReason != null;
        }

        List<ReasoningStep> reasoningTrace() {
            return reasoningTrace;
        }

        List<ToolExecutionRecord> toolExecutions() {
            return toolExecutions;
        }

        List<String> observations() {
            return observations;
        }

        Set<String> usedTools() {
            return usedTools;
        }

        AgentFinalizePayload finalPayload() {
            return finalPayload;
        }

        String failureReason() {
            return failureReason;
        }
    }
}

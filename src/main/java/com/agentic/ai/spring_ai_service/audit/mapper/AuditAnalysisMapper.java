package com.agentic.ai.spring_ai_service.audit.mapper;

import com.agentic.ai.spring_ai_service.audit.dto.response.*;
import com.agentic.ai.spring_ai_service.audit.model.*;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AuditAnalysisMapper {

    public AuditAnalysisResponseDto toDto(AuditAiAnalysis analysis) {
        if (analysis == null) {
            return null;
        }

        return AuditAnalysisResponseDto.builder()
                .analysisId(analysis.getId())
                .eventId(analysis.getEventId())
                .riskScore(analysis.getRiskScore())
                .category(analysis.getCategory())
                .confidenceScore(analysis.getConfidenceScore())
                .confidenceLabel(analysis.getConfidenceLabel())
                .summary(analysis.getSummary())
                .reasons(defaultList(analysis.getReasons()))
                .tags(defaultList(analysis.getTags()))
                .recommendedAction(analysis.getRecommendedAction())
                .grounded(analysis.getGrounded())
                .fallbackUsed(analysis.getFallbackUsed())
                .toolsInvoked(analysis.getToolsInvoked())
                .analysisSucceeded(analysis.getAnalysisSucceeded())
                .modelName(analysis.getModelName())
                .analysisVersion(analysis.getAnalysisVersion())
                .analyzedAt(analysis.getAnalyzedAt())
                .matchedPolicyEvidence(toEvidenceDtos(analysis.getMatchedPolicyEvidence()))
                .toolExecutions(toToolDtos(analysis.getToolExecutions()))
                .diagnostics(toDiagnosticsDto(analysis.getDiagnostics()))
                .reasoningTrace(toReasoningDtos(analysis.getReasoningTrace()))
                .build();
    }

    private List<MatchedPolicyEvidenceDto> toEvidenceDtos(List<MatchedPolicyEvidence> items) {
        return defaultList(items).stream()
                .map(item -> MatchedPolicyEvidenceDto.builder()
                        .policyName(item.getPolicyName())
                        .excerpt(item.getExcerpt())
                        .relevanceScore(item.getRelevanceScore())
                        .sourceChunkId(item.getSourceChunkId())
                        .build())
                .collect(Collectors.toList());
    }

    private List<ToolExecutionDto> toToolDtos(List<ToolExecutionRecord> items) {
        return defaultList(items).stream()
                .map(item -> ToolExecutionDto.builder()
                        .toolName(item.getToolName())
                        .success(item.getSuccess())
                        .durationMs(item.getDurationMs())
                        .inputSummary(item.getInputSummary())
                        .outputSummary(item.getOutputSummary())
                        .errorMessage(item.getErrorMessage())
                        .executedAt(item.getExecutedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private DiagnosticsDto toDiagnosticsDto(AnalysisDiagnostics diagnostics) {
        if (diagnostics == null) {
            return null;
        }

        return DiagnosticsDto.builder()
                .retrievedChunkCount(diagnostics.getRetrievedChunkCount())
                .evidenceUsedCount(diagnostics.getEvidenceUsedCount())
                .toolsRequestedCount(diagnostics.getToolsRequestedCount())
                .toolsExecutedCount(diagnostics.getToolsExecutedCount())
                .reasoningIterations(diagnostics.getReasoningIterations())
                .maxReasoningIterations(diagnostics.getMaxReasoningIterations())
                .reasoningTruncated(diagnostics.getReasoningTruncated())
                .orchestrationMode(diagnostics.getOrchestrationMode())
                .fallbackReason(diagnostics.getFallbackReason())
                .validatorStatus(diagnostics.getValidatorStatus())
                .scoringNotes(diagnostics.getScoringNotes())
                .build();
    }

    private List<ReasoningStepDto> toReasoningDtos(List<ReasoningStep> items) {
        return defaultList(items).stream()
                .map(item -> ReasoningStepDto.builder()
                        .stepNumber(item.getStepNumber())
                        .thought(item.getThought())
                        .action(item.getAction())
                        .observation(item.getObservation())
                        .decision(item.getDecision())
                        .timestamp(item.getTimestamp())
                        .build())
                .collect(Collectors.toList());
    }

    private <T> List<T> defaultList(List<T> items) {
        return items == null ? Collections.emptyList() : items;
    }
}
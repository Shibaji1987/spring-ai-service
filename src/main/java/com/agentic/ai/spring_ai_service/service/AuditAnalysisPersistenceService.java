package com.agentic.ai.spring_ai_service.service;

import com.agentic.ai.spring_ai_service.audit.dto.agent.AgentFinalizePayload;
import com.agentic.ai.spring_ai_service.audit.model.*;
import com.agentic.ai.spring_ai_service.audit.repository.AuditAiAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditAnalysisPersistenceService {

    private final AuditAiAnalysisRepository repository;

    public AuditAiAnalysis upsertAnalysis(
            String eventId,
            AgentFinalizePayload finalPayload,
            List<MatchedPolicyEvidence> matchedPolicyEvidence,
            List<ToolExecutionRecord> toolExecutions,
            List<ReasoningStep> reasoningTrace,
            AnalysisDiagnostics diagnostics,
            boolean grounded,
            boolean fallbackUsed,
            boolean toolsInvoked,
            boolean analysisSucceeded,
            String modelName,
            String analysisVersion
    ) {
        AuditAiAnalysis analysis = repository.findByEventId(eventId)
                .orElseGet(AuditAiAnalysis::new);

        analysis.setEventId(eventId);
        analysis.setRiskScore(finalPayload.getRiskScore());
        analysis.setCategory(finalPayload.getCategory());
        analysis.setConfidenceScore(finalPayload.getConfidenceScore());
        analysis.setConfidenceLabel(finalPayload.getConfidenceLabel());
        analysis.setSummary(finalPayload.getSummary());
        analysis.setReasons(defaultList(finalPayload.getReasons()));
        analysis.setTags(defaultList(finalPayload.getTags()));
        analysis.setRecommendedAction(finalPayload.getRecommendedAction());

        analysis.setGrounded(grounded);
        analysis.setFallbackUsed(fallbackUsed);
        analysis.setToolsInvoked(toolsInvoked);
        analysis.setAnalysisSucceeded(analysisSucceeded);
        analysis.setModelName(modelName);
        analysis.setAnalysisVersion(analysisVersion);
        analysis.setAnalyzedAt(Instant.now());

        analysis.setMatchedPolicyEvidence(defaultList(matchedPolicyEvidence));
        analysis.setToolExecutions(defaultList(toolExecutions));
        analysis.setDiagnostics(diagnostics);
        analysis.setReasoningTrace(defaultList(reasoningTrace));

        return repository.save(analysis);
    }

    private <T> List<T> defaultList(List<T> items) {
        return items == null ? Collections.emptyList() : items;
    }
}
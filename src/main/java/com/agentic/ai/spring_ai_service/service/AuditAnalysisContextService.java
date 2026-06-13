package com.agentic.ai.spring_ai_service.service;

import com.agentic.ai.spring_ai_service.audit.model.AuditEvent;
import com.agentic.ai.spring_ai_service.audit.model.KnowledgeChunk;
import com.agentic.ai.spring_ai_service.audit.model.MatchedPolicyEvidence;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditAnalysisContextService {

    private static final int POLICY_EVIDENCE_LIMIT = 3;
    private static final int EVIDENCE_EXCERPT_LIMIT = 500;

    private final AuditEventService auditEventService;
    private final KnowledgeRetrievalService knowledgeRetrievalService;

    public AuditAnalysisContext load(String eventId) {
        AuditEvent auditEvent = auditEventService.getEventById(eventId);
        return new AuditAnalysisContext(auditEvent, retrievePolicyEvidence(auditEvent));
    }

    private List<MatchedPolicyEvidence> retrievePolicyEvidence(AuditEvent event) {
        try {
            return knowledgeRetrievalService
                    .findTopKRelevantChunks(toRetrievalQuery(event), POLICY_EVIDENCE_LIMIT)
                    .stream()
                    .map(this::toEvidence)
                    .toList();
        } catch (Exception ex) {
            log.warn("Policy retrieval failed for LLM analysis. error={}", ex.getMessage());
            return List.of();
        }
    }

    private MatchedPolicyEvidence toEvidence(KnowledgeChunk chunk) {
        return MatchedPolicyEvidence.builder()
                .policyName(safe(chunk.getDocumentTitle()))
                .excerpt(truncate(chunk.getText(), EVIDENCE_EXCERPT_LIMIT))
                .relevanceScore(null)
                .sourceChunkId(chunk.getId())
                .build();
    }

    private String toRetrievalQuery(AuditEvent event) {
        return """
                Event Type: %s
                Actor: %s
                Action: %s
                Target: %s
                Status: %s
                Event Time: %s
                Metadata: %s
                """.formatted(
                safe(event.getEventType()),
                safe(event.getActor()),
                safe(event.getAction()),
                safe(event.getTarget()),
                safe(event.getStatus()),
                event.getEventTime(),
                event.getMetadata()
        );
    }

    private String truncate(String value, int maxLength) {
        String safeValue = safe(value);
        return safeValue.length() <= maxLength ? safeValue : safeValue.substring(0, maxLength);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}

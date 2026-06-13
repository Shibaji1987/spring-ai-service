package com.agentic.ai.spring_ai_service.audit.tools;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record InvestigationEvidence(
        String source,
        Instant observedAt,
        double confidence,
        List<String> evidenceIds,
        Map<String, Object> facts,
        List<String> limitations
) {
    public static InvestigationEvidence of(
            String source,
            double confidence,
            List<String> evidenceIds,
            Map<String, Object> facts,
            List<String> limitations
    ) {
        return new InvestigationEvidence(
                source,
                Instant.now(),
                Math.max(0.0, Math.min(confidence, 1.0)),
                evidenceIds == null ? List.of() : List.copyOf(evidenceIds),
                facts == null ? Map.of() : Map.copyOf(facts),
                limitations == null ? List.of() : List.copyOf(limitations)
        );
    }
}

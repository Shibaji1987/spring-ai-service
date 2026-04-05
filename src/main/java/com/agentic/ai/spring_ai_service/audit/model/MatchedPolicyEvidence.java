package com.agentic.ai.spring_ai_service.audit.model;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchedPolicyEvidence {
    private String policyName;
    private String excerpt;
    private Double relevanceScore;
    private String sourceChunkId;
}
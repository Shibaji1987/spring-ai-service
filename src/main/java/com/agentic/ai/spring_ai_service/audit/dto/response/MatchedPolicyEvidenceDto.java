package com.agentic.ai.spring_ai_service.audit.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchedPolicyEvidenceDto {
    private String policyName;
    private String excerpt;
    private Double relevanceScore;
    private String sourceChunkId;
}
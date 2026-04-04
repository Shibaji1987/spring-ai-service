package com.agentic.ai.spring_ai_service.audit.model;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchedPolicyEvidence {

    private String documentId;
    private String chunkId;
    private String documentName;
    private String policyCode;
    private String title;

    private String excerpt;
    private Double retrievalScore;
    private Integer rank;
    private String rationale;   // why this chunk mattered to the analysis
}
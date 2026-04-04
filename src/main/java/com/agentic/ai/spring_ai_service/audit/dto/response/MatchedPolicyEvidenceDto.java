package com.agentic.ai.spring_ai_service.audit.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchedPolicyEvidenceDto {
    private String title;
    private String documentName;
    private String excerpt;
    private Double retrievalScore;
    private String rationale;
}
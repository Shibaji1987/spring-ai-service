package com.agentic.ai.spring_ai_service.audit.model;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyEvidenceRef {
    private String chunkId;
    private String rationale;
}
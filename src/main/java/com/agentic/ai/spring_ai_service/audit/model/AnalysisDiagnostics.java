package com.agentic.ai.spring_ai_service.audit.model;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisDiagnostics {

    private Integer retrievedChunkCount;
    private Integer evidenceUsedCount;
    private Integer toolsRequestedCount;
    private Integer toolsExecutedCount;

    private String fallbackReason;
    private String validatorStatus;
    private String scoringNotes;
}
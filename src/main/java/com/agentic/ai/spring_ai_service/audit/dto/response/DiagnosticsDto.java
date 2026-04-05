package com.agentic.ai.spring_ai_service.audit.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosticsDto {
    private Integer retrievedChunkCount;
    private Integer evidenceUsedCount;
    private Integer toolsRequestedCount;
    private Integer toolsExecutedCount;

    private Integer reasoningIterations;
    private Integer maxReasoningIterations;
    private Boolean reasoningTruncated;

    private String orchestrationMode;
    private String fallbackReason;
    private String validatorStatus;
    private String scoringNotes;
}
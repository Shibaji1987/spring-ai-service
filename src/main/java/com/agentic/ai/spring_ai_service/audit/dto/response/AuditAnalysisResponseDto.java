package com.agentic.ai.spring_ai_service.audit.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditAnalysisResponseDto {
    private String analysisId;
    private String eventId;

    private Integer riskScore;
    private String category;

    private Double confidenceScore;
    private String confidenceLabel;

    private String summary;
    private List<String> reasons;
    private List<String> tags;
    private String recommendedAction;

    private Boolean grounded;
    private Boolean fallbackUsed;
    private Boolean toolsInvoked;
    private Boolean analysisSucceeded;

    private String modelName;
    private String analysisVersion;
    private Instant analyzedAt;

    private List<MatchedPolicyEvidenceDto> matchedPolicyEvidence;
    private List<ToolExecutionDto> toolExecutions;
    private DiagnosticsDto diagnostics;
    private List<ReasoningStepDto> reasoningTrace;
}
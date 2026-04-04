package com.example.audit.dto.response;

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
    private Instant analyzedAt;

    private List<MatchedPolicyEvidenceDto> evidence;
    private List<ToolExecutionDto> toolExecutions;
}
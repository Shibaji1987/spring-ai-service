package com.agentic.ai.spring_ai_service.audit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditAnalysisStreamEventDto {
    private String eventId;
    private String phase;
    private String status;
    private String message;
    private List<MatchedPolicyEvidenceDto> matchedPolicyEvidence;
    private ToolExecutionDto toolExecution;
    private AuditAnalysisResponseDto analysis;
    private Instant timestamp;
}

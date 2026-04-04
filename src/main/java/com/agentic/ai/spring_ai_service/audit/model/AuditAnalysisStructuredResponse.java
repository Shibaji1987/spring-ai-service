package com.agentic.ai.spring_ai_service.audit.model;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditAnalysisStructuredResponse {

    @Min(0) @Max(100)
    private Integer riskScore;

    @NotBlank
    private String category;

    @DecimalMin("0.0") @DecimalMax("1.0")
    private Double confidenceScore;

    @NotBlank
    private String summary;

    @Size(min = 1, max = 10)
    private List<String> reasons;

    @Size(max = 10)
    private List<String> tags;

    @NotBlank
    private String recommendedAction;

    private Boolean needsInvestigation;
    private List<ToolCallInstruction> requestedInvestigations;

    @Size(max = 10)
    private List<PolicyEvidenceRef> evidenceRefs;
}
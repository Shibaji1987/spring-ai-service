package com.agentic.ai.spring_ai_service.audit.model;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditAnalysisStructuredResponse {

    @Min(0)
    @Max(100)
    private Integer riskScore;

    @NotBlank
    private String category;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
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
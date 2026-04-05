package com.agentic.ai.spring_ai_service.audit.dto.agent;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentFinalizePayload {
    private Integer riskScore;
    private String category;
    private String summary;
    private List<String> reasons;
    private List<String> tags;
    private String recommendedAction;

    private Double confidenceScore;
    private String confidenceLabel;
    private Boolean fallbackUsed;
}
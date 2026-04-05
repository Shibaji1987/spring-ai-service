package com.agentic.ai.spring_ai_service.audit.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReasoningStep {
    private Integer stepNumber;
    private String thought;      // short audit-safe rationale
    private String action;       // TOOL:getFailedLoginCount / RETRIEVE_POLICY / FINALIZE
    private String observation;  // summarized result
    private String decision;     // continue / stop / escalate / benign
    private LocalDateTime timestamp;
}
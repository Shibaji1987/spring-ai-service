package com.agentic.ai.spring_ai_service.audit.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReasoningStepDto {
    private Integer stepNumber;
    private String thought;
    private String action;
    private String observation;
    private String decision;
    private LocalDateTime timestamp;
}
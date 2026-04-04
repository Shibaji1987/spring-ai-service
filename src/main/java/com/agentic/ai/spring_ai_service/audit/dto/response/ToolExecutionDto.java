package com.agentic.ai.spring_ai_service.audit.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolExecutionDto {
    private String toolName;
    private String status;
    private Long durationMs;
    private String failureReason;
}
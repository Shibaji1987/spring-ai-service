package com.agentic.ai.spring_ai_service.audit.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolExecutionRecord {
    private String toolName;
    private Boolean success;
    private Long durationMs;
    private String inputSummary;
    private String outputSummary;
    private String errorMessage;
    private LocalDateTime executedAt;
}
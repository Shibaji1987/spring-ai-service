package com.agentic.ai.spring_ai_service.audit.model;

import lombok.*;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolExecutionRecord {

    private String toolName;
    private String status; // REQUESTED, EXECUTED, FAILED, SKIPPED
    private Instant startedAt;
    private Instant completedAt;

    private Map<String, Object> input;
    private Map<String, Object> output;

    private String failureReason;
    private Long durationMs;
}
package com.agentic.ai.spring_ai_service.audit.model;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolCallInstruction {
    private String toolName;
    private Map<String, Object> arguments;
    private String reason;
}
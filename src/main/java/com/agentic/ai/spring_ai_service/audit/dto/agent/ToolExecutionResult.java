package com.agentic.ai.spring_ai_service.audit.dto.agent;

public record ToolExecutionResult(
        String toolName,
        boolean success,
        Object data,
        String errorMessage,
        long durationMs
) {
    public static ToolExecutionResult success(String toolName, Object data, long durationMs) {
        return new ToolExecutionResult(toolName, true, data, null, durationMs);
    }

    public static ToolExecutionResult failure(String toolName, String errorMessage, long durationMs) {
        return new ToolExecutionResult(toolName, false, null, errorMessage, durationMs);
    }
}
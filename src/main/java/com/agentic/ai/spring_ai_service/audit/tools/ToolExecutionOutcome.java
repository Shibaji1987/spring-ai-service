package com.agentic.ai.spring_ai_service.audit.tools;

import com.agentic.ai.spring_ai_service.audit.model.ToolExecutionRecord;

public record ToolExecutionOutcome(
        ToolExecutionRecord executionRecord,
        String summaryText
) {
}
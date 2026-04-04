package com.agentic.ai.spring_ai_service.audit.model;


import com.agentic.ai.spring_ai_service.audit.model.ToolExecutionRecord;

import java.util.List;

public record ToolOrchestrationResult(
        boolean toolsInvoked,
        List<ToolExecutionRecord> executionRecords,
        List<String> toolEvidenceSummaries
) {}
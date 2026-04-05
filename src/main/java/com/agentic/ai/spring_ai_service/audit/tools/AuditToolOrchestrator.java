package com.agentic.ai.spring_ai_service.audit.tools;

import com.agentic.ai.spring_ai_service.audit.model.ToolExecutionRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuditToolOrchestrator {

    private final InvestigationToolGateway investigationToolGateway;

    public List<ToolExecutionRecord> executeTools(List<String> toolNames, Map<String, Object> input) {
        List<ToolExecutionRecord> executions = new ArrayList<>();

        if (toolNames == null || toolNames.isEmpty()) {
            return executions;
        }

        for (String toolName : toolNames) {
            ToolExecutionRecord result =
                    investigationToolGateway.executeWhitelisted(toolName, input);

            executions.add(result);
        }

        return executions;
    }
}
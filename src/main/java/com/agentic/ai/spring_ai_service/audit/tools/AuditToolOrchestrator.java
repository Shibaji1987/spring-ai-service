package com.agentic.ai.spring_ai_service.audit.tools;

import com.agentic.ai.spring_ai_service.audit.model.AuditAnalysisStructuredResponse;
import com.agentic.ai.spring_ai_service.audit.model.ToolExecutionRecord;
import com.agentic.ai.spring_ai_service.audit.model.ToolOrchestrationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditToolOrchestrator {

    private final InvestigationToolGateway investigationToolGateway;

    public ToolOrchestrationResult executeRequestedTools(AuditContext context,
                                                         AuditAnalysisStructuredResponse initialResponse) {

        List<ToolExecutionRecord> executions = new ArrayList<>();
        List<String> toolSummaries = new ArrayList<>();

        if (initialResponse.getNeedsInvestigation() == null || !initialResponse.getNeedsInvestigation()) {
            return new ToolOrchestrationResult(false, executions, toolSummaries);
        }

        if (initialResponse.getRequestedInvestigations() == null) {
            return new ToolOrchestrationResult(false, executions, toolSummaries);
        }

        for (var request : initialResponse.getRequestedInvestigations()) {
            var result = investigationToolGateway.executeWhitelisted(
                    request.getToolName(),
                    request.getArguments()
            );

            executions.add(result.executionRecord());
            if (result.summaryText() != null) {
                toolSummaries.add(result.summaryText());
            }
        }

        return new ToolOrchestrationResult(!executions.isEmpty(), executions, toolSummaries);
    }
}
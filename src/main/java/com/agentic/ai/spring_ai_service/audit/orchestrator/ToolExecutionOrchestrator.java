package com.agentic.ai.spring_ai_service.audit.orchestrator;

import com.agentic.ai.spring_ai_service.audit.dto.agent.AgentToolRequest;
import com.agentic.ai.spring_ai_service.audit.model.ToolExecutionRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ToolExecutionOrchestrator {

    public ToolExecutionRecord execute(Object auditEvent, AgentToolRequest request) {
        long start = System.currentTimeMillis();

        try {
            String toolName = request != null ? request.getToolName() : "unknown-tool";

            String output = "Executed " + toolName + " successfully.";
            long duration = System.currentTimeMillis() - start;

            return ToolExecutionRecord.builder()
                    .toolName(toolName)
                    .success(true)
                    .durationMs(duration)
                    .inputSummary(request != null && request.getToolArgs() != null ? request.getToolArgs().toString() : "{}")
                    .outputSummary(output)
                    .errorMessage(null)
                    .executedAt(LocalDateTime.now())
                    .build();

        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - start;

            return ToolExecutionRecord.builder()
                    .toolName(request != null ? request.getToolName() : "unknown-tool")
                    .success(false)
                    .durationMs(duration)
                    .inputSummary(request != null && request.getToolArgs() != null ? request.getToolArgs().toString() : "{}")
                    .outputSummary(null)
                    .errorMessage(ex.getMessage())
                    .executedAt(LocalDateTime.now())
                    .build();
        }
    }
}
package com.agentic.ai.spring_ai_service.audit.orchestrator;

import com.agentic.ai.spring_ai_service.audit.dto.agent.AgentToolRequest;
import com.agentic.ai.spring_ai_service.audit.model.AuditEvent;
import com.agentic.ai.spring_ai_service.audit.model.ToolExecutionRecord;
import com.agentic.ai.spring_ai_service.audit.tools.InvestigationToolGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ToolExecutionOrchestrator {

    private final InvestigationToolGateway investigationToolGateway;

    public ToolExecutionRecord execute(Object auditEvent, AgentToolRequest request) {
        AuditEvent event = (AuditEvent) auditEvent;

        String toolName = request != null ? request.getToolName() : "unknown-tool";

        log.info("[TOOL-ORCH] executing tool={} actor={} args={}",
                toolName,
                event != null ? event.getActor() : null,
                request != null ? request.getToolArgs() : "{}");

        ToolExecutionRecord record = investigationToolGateway.executeWhitelisted(
                toolName,
                request != null ? request.getToolArgs() : null
        );

        log.info("[TOOL-ORCH] result tool={} success={} durationMs={} output='{}' error='{}'",
                record.getToolName(),
                record.getSuccess(),
                record.getDurationMs(),
                record.getOutputSummary(),
                record.getErrorMessage());

        return record;
    }
}
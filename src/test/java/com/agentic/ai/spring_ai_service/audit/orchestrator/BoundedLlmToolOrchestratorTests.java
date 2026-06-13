package com.agentic.ai.spring_ai_service.audit.orchestrator;

import com.agentic.ai.spring_ai_service.audit.dto.agent.AgentDecision;
import com.agentic.ai.spring_ai_service.audit.dto.agent.AgentFinalizePayload;
import com.agentic.ai.spring_ai_service.audit.dto.agent.AgentToolRequest;
import com.agentic.ai.spring_ai_service.audit.dto.response.AuditAnalysisStreamEventDto;
import com.agentic.ai.spring_ai_service.audit.mapper.AuditAnalysisMapper;
import com.agentic.ai.spring_ai_service.audit.model.AuditEvent;
import com.agentic.ai.spring_ai_service.audit.model.ToolExecutionRecord;
import com.agentic.ai.spring_ai_service.service.AnalysisConfidenceService;
import com.agentic.ai.spring_ai_service.service.AnalysisResponseValidator;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BoundedLlmToolOrchestratorTests {

    private final AgentDecisionService decisionService = mock(AgentDecisionService.class);
    private final ToolExecutionOrchestrator toolOrchestrator = mock(ToolExecutionOrchestrator.class);
    private final BoundedLlmToolOrchestrator orchestrator = new BoundedLlmToolOrchestrator(
            decisionService,
            toolOrchestrator,
            new AnalysisConfidenceService(),
            new AnalysisResponseValidator(),
            new AuditAnalysisMapper()
    );

    @Test
    void returnsToolObservationToTheLlmBeforeFinalization() {
        AuditEvent event = auditEvent();
        AgentDecision toolDecision = toolDecision("getRecentEvents");
        AgentDecision finalDecision = finalDecision();

        when(decisionService.decide(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(toolDecision, finalDecision);
        when(toolOrchestrator.execute(any(), any())).thenReturn(successfulToolRecord("getRecentEvents"));

        List<AuditAnalysisStreamEventDto> events = new ArrayList<>();
        ReActExecutionResult result = orchestrator.execute("event-1", event, List.of(), events::add);

        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(result.getToolExecutions()).hasSize(1);
        assertThat(result.getReasoningTrace()).hasSize(2);
        assertThat(result.getFinalPayload().getCategory()).isEqualTo("SUSPICIOUS_LOGIN");
        assertThat(events).extracting(AuditAnalysisStreamEventDto::getPhase)
                .contains("LLM_DECISION", "TOOL_REQUESTED", "TOOL_EXECUTION", "AI_REASONING");
        verify(toolOrchestrator).execute(event, toolDecision.getToolRequest());
    }

    private AuditEvent auditEvent() {
        AuditEvent event = new AuditEvent();
        event.setActor("analyst@bank.com");
        event.setEventType("LOGIN");
        event.setStatus("FAILURE");
        return event;
    }

    private AgentDecision toolDecision(String toolName) {
        return AgentDecision.builder()
                .thought("Recent activity is required to assess the failed login.")
                .action("TOOL")
                .decision("continue")
                .toolRequest(AgentToolRequest.builder()
                        .toolName(toolName)
                        .toolArgs(Map.of("actor", "analyst@bank.com", "limit", 5))
                        .build())
                .build();
    }

    private AgentDecision finalDecision() {
        return AgentDecision.builder()
                .thought("The event and recent activity provide enough evidence.")
                .action("FINALIZE")
                .decision("stop")
                .finalResponse(AgentFinalizePayload.builder()
                        .riskScore(7)
                        .category("SUSPICIOUS_LOGIN")
                        .summary("The failed login requires investigation.")
                        .reasons(List.of("failed_login"))
                        .tags(List.of("login", "review"))
                        .recommendedAction("Verify the user's identity.")
                        .fallbackUsed(false)
                        .build())
                .build();
    }

    private ToolExecutionRecord successfulToolRecord(String toolName) {
        return ToolExecutionRecord.builder()
                .toolName(toolName)
                .success(true)
                .inputSummary("{actor=analyst@bank.com, limit=5}")
                .outputSummary("Recent events show three failed logins.")
                .durationMs(12L)
                .executedAt(LocalDateTime.now())
                .build();
    }
}

package com.agentic.ai.spring_ai_service.audit.orchestrator;

import com.agentic.ai.spring_ai_service.audit.dto.agent.AgentDecision;
import com.agentic.ai.spring_ai_service.audit.dto.agent.AgentToolRequest;
import com.agentic.ai.spring_ai_service.audit.model.AuditEvent;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentDecisionValidatorTests {

    private final AgentDecisionValidator validator = new AgentDecisionValidator();

    @Test
    void normalizesToolArgumentsToTheCurrentAuditActor() {
        AuditEvent event = new AuditEvent();
        event.setActor("trusted.actor@bank.com");

        AgentDecision decision = AgentDecision.builder()
                .thought(" Recent history is required. ")
                .action("TOOL")
                .toolRequest(AgentToolRequest.builder()
                        .toolName("getRecentEvents")
                        .toolArgs(Map.of("actor", "different.actor@bank.com", "limit", 500))
                        .build())
                .build();

        AgentDecision validated = validator.validate(decision, event, 1, 5);

        assertThat(validated.getToolRequest().getToolArgs())
                .containsEntry("actor", "trusted.actor@bank.com")
                .containsEntry("limit", 10);
        assertThat(validated.getThought()).isEqualTo("Recent history is required.");
        assertThat(validated.getDecision()).isEqualTo("continue");
    }

    @Test
    void rejectsToolsOutsideTheAllowlist() {
        AuditEvent event = new AuditEvent();
        event.setActor("analyst@bank.com");
        AgentDecision decision = AgentDecision.builder()
                .action("TOOL")
                .toolRequest(AgentToolRequest.builder()
                        .toolName("deleteAuditEvents")
                        .toolArgs(Map.of())
                        .build())
                .build();

        assertThatThrownBy(() -> validator.validate(decision, event, 1, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("allowlisted");
    }
}

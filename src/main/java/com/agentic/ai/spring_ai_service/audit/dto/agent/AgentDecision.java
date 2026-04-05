package com.agentic.ai.spring_ai_service.audit.dto.agent;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentDecision {
    private String thought;              // short bounded rationale
    private String action;               // TOOL or FINALIZE
    private String decision;             // continue / stop / escalate / benign
    private AgentToolRequest toolRequest;
    private AgentFinalizePayload finalResponse;
}
package com.agentic.ai.spring_ai_service.audit.orchestrator;

import com.agentic.ai.spring_ai_service.audit.dto.agent.AgentFinalizePayload;
import com.agentic.ai.spring_ai_service.audit.model.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReActExecutionResult {
    private String eventId;
    private AgentFinalizePayload finalPayload;
    private List<MatchedPolicyEvidence> matchedPolicyEvidence;
    private List<ToolExecutionRecord> toolExecutions;
    private List<ReasoningStep> reasoningTrace;
    private AnalysisDiagnostics diagnostics;
    private boolean grounded;
    private boolean fallbackUsed;
    private boolean toolsInvoked;
    private boolean analysisSucceeded;
}
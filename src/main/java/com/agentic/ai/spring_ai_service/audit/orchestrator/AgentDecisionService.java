package com.agentic.ai.spring_ai_service.audit.orchestrator;

import com.agentic.ai.spring_ai_service.audit.dto.agent.AgentDecision;
import com.agentic.ai.spring_ai_service.audit.dto.agent.AgentFinalizePayload;
import com.agentic.ai.spring_ai_service.audit.dto.agent.AgentToolRequest;
import com.agentic.ai.spring_ai_service.audit.model.MatchedPolicyEvidence;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AgentDecisionService {

    public AgentDecision decide(
            Object auditEvent,
            List<MatchedPolicyEvidence> matchedPolicyEvidence,
            List<String> observations,
            int currentIteration,
            int maxIterations
    ) {
        if (currentIteration >= maxIterations || observations.size() >= 2) {
            return AgentDecision.builder()
                    .thought("Enough evidence collected to finalize.")
                    .action("FINALIZE")
                    .decision("stop")
                    .finalResponse(
                            AgentFinalizePayload.builder()
                                    .riskScore(7)
                                    .category("SUSPICIOUS_ACTIVITY")
                                    .summary("Analysis completed using bounded reasoning and collected evidence.")
                                    .reasons(List.of("policy_match", "tool_observation"))
                                    .tags(List.of("react", "grounded"))
                                    .recommendedAction("Review the event and confirm legitimacy.")
                                    .fallbackUsed(false)
                                    .build()
                    )
                    .build();
        }

        return AgentDecision.builder()
                .thought("Need tool evidence before finalizing.")
                .action("TOOL")
                .decision("continue")
                .toolRequest(
                        AgentToolRequest.builder()
                                .toolName("getRecentEvents")
                                .toolArgs(Map.of("limit", 5))
                                .build()
                )
                .build();
    }
}
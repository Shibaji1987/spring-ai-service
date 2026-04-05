package com.agentic.ai.spring_ai_service.audit.orchestrator;

import com.agentic.ai.spring_ai_service.audit.dto.agent.AgentDecision;
import com.agentic.ai.spring_ai_service.audit.dto.agent.AgentToolRequest;
import com.agentic.ai.spring_ai_service.audit.model.AuditEvent;
import com.agentic.ai.spring_ai_service.audit.model.MatchedPolicyEvidence;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentDecisionService {

    private final ChatClient.Builder chatClientBuilder;

    public AgentDecision decide(
            Object auditEvent,
            List<MatchedPolicyEvidence> matchedPolicyEvidence,
            List<String> observations,
            int currentIteration,
            int maxIterations
    ) {
        AuditEvent event = (AuditEvent) auditEvent;

        ChatClient chatClient = chatClientBuilder.build();

        String prompt = buildPrompt(
                event,
                matchedPolicyEvidence,
                observations,
                currentIteration,
                maxIterations
        );

        log.info("[LLM-DECISION][step={}] sending prompt for actor={} action={} status={}",
                currentIteration,
                event != null ? event.getActor() : null,
                event != null ? event.getAction() : null,
                event != null ? event.getStatus() : null);

        AgentDecision decision = chatClient.prompt()
                .user(prompt)
                .call()
                .entity(AgentDecision.class);

        log.info("[LLM-DECISION][step={}] thought='{}' action={} tool={} decision={}",
                currentIteration,
                decision != null ? decision.getThought() : null,
                decision != null ? decision.getAction() : null,
                decision != null && decision.getToolRequest() != null ? decision.getToolRequest().getToolName() : null,
                decision != null ? decision.getDecision() : null);

        return decision;
    }

    private String buildPrompt(
            AuditEvent event,
            List<MatchedPolicyEvidence> evidence,
            List<String> observations,
            int step,
            int maxIterations
    ) {
        return """
You are an enterprise audit investigation agent.

You must decide the NEXT BEST STEP only.
Return ONLY JSON matching this structure:

{
  "thought": "short operational rationale",
  "action": "TOOL or FINALIZE",
  "decision": "continue or stop",
  "toolRequest": {
    "toolName": "getRecentEvents or getFailedLoginCount or getUserActivitySummary",
    "toolArgs": {
      "actor": "string",
      "limit": 5
    }
  },
  "finalResponse": null
}

Rules:
1. Keep thought short and audit-safe.
2. Do NOT generate a final analysis unless enough evidence exists.
3. Use only these tools:
   - getRecentEvents
   - getFailedLoginCount
   - getUserActivitySummary
4. If event is a normal successful login with no suspicious signals, prefer low-risk finalization after minimal evidence.
5. If event is a failed login, suspicious sequence, or unusual admin activity, gather evidence first.
6. Never exceed max iterations.
7. If enough evidence exists, return action=FINALIZE.
8. If you return FINALIZE, set toolRequest to null.
9. Do not include markdown.

Current step: %d
Max steps: %d

Event:
- eventType: %s
- actor: %s
- action: %s
- target: %s
- status: %s
- eventTime: %s
- metadata: %s

Matched policy evidence:
%s

Previous observations:
%s
""".formatted(
                step,
                maxIterations,
                safe(event != null ? event.getEventType() : null),
                safe(event != null ? event.getActor() : null),
                safe(event != null ? event.getAction() : null),
                safe(event != null ? event.getTarget() : null),
                safe(event != null ? event.getStatus() : null),
                event != null ? event.getEventTime() : null,
                event != null ? event.getMetadata() : null,
                evidence,
                observations
        );
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
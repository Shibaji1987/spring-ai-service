package com.agentic.ai.spring_ai_service.service;

import com.agentic.ai.spring_ai_service.audit.dto.agent.AgentFinalizePayload;
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
public class AuditAiService {

    private final ChatClient.Builder chatClientBuilder;

    /**
     * Backward compatibility for existing AuditAnalysisService
     */
    public String analyze(AuditEvent event) {
        AgentFinalizePayload payload = generateFinalAnalysis(
                event,
                List.of(),
                List.of(),
                false
        );

        return payload != null ? payload.toString() : "{}";
    }

    /**
     * New final analysis method used by bounded ReAct flow
     */
    public AgentFinalizePayload generateFinalAnalysis(
            AuditEvent event,
            List<MatchedPolicyEvidence> matchedPolicyEvidence,
            List<String> observations,
            boolean fallbackUsed
    ) {
        ChatClient chatClient = chatClientBuilder.build();

        String prompt = buildFinalPrompt(
                event,
                matchedPolicyEvidence,
                observations,
                fallbackUsed
        );

        log.info(
                "[LLM-FINAL] generating final analysis for eventId={} actor={} action={}",
                event != null ? event.getId() : null,
                event != null ? event.getActor() : null,
                event != null ? event.getAction() : null
        );

        AgentFinalizePayload payload = chatClient.prompt()
                .user(prompt)
                .call()
                .entity(AgentFinalizePayload.class);

        if (payload != null) {
            payload.setFallbackUsed(fallbackUsed);
        }

        log.info(
                "[LLM-FINAL] result riskScore={} category={} fallbackUsed={}",
                payload != null ? payload.getRiskScore() : null,
                payload != null ? payload.getCategory() : null,
                payload != null ? payload.getFallbackUsed() : null
        );

        return payload;
    }

    private String buildFinalPrompt(
            AuditEvent event,
            List<MatchedPolicyEvidence> evidence,
            List<String> observations,
            boolean fallbackUsed
    ) {
        return """
                You are an enterprise audit risk analysis engine.
                
                Generate the FINAL audit analysis.
                Return ONLY JSON matching this structure:
                
                {
                  "riskScore": 0,
                  "category": "BENIGN_LOGIN",
                  "summary": "text",
                  "reasons": ["text"],
                  "tags": ["text"],
                  "recommendedAction": "text",
                  "confidenceScore": null,
                  "confidenceLabel": null,
                  "fallbackUsed": false
                }
                
                Rules:
                1. riskScore must be from 0 to 10.
                2. Use BENIGN_LOGIN for normal successful low-risk logins.
                3. Use SUSPICIOUS_LOGIN for suspicious login activity.
                4. Use UNUSUAL_ADMIN_ACTION for risky admin actions.
                5. Use REVIEW_REQUIRED if evidence is mixed or incomplete.
                6. Be conservative and realistic.
                7. Do not inflate benign events into suspicious ones.
                8. Do not include markdown.
                
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
                
                Tool/Reasoning observations:
                %s
                
                Fallback used:
                %s
                """.formatted(
                safe(event != null ? event.getEventType() : null),
                safe(event != null ? event.getActor() : null),
                safe(event != null ? event.getAction() : null),
                safe(event != null ? event.getTarget() : null),
                safe(event != null ? event.getStatus() : null),
                event != null ? event.getEventTime() : null,
                event != null ? event.getMetadata() : null,
                evidence,
                observations,
                fallbackUsed
        );
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
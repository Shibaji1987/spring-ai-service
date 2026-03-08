package com.agentic.ai.spring_ai_service.service;


import com.agentic.ai.spring_ai_service.audit.dto.AuditHistoryStore;
import com.agentic.ai.spring_ai_service.audit.tools.AuditTools;
import com.agentic.ai.spring_ai_service.audit.dto.AuditAnalyzeResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AuditAgentService {

    private final ChatClient chatClient;
    private final AuditTools auditTools;
    private final AuditHistoryStore auditHistoryStore;

    public AuditAgentService(ChatClient.Builder builder,
                             AuditTools auditTools,
                             AuditHistoryStore auditHistoryStore) {
        this.chatClient = builder.build();
        this.auditTools = auditTools;
        this.auditHistoryStore = auditHistoryStore;
    }

    public AuditAnalyzeResponse analyzeEvent(String event) {
        AuditAnalyzeResponse response = chatClient.prompt()
                .system("""
                        You are an Audit Security Analyst AI.
                        Return ONLY the structured output matching the response schema.

                        CATEGORY MUST BE EXACTLY ONE OF:
                        - suspicious_login
                        - data_access
                        - privilege_change
                        - admin_action
                        - policy_violation
                        - benign
                        - unknown

                        Constraints:
                        - riskScore must be an integer from 0 to 10
                        - summary must be short
                        - reasons must contain 3 to 6 short items
                        - tags must be lowercase with underscores where needed
                        - recommendedAction must be one short sentence
                        """)
                .user(u -> u.text("""
                        Analyze this audit event:

                        EVENT:
                        {event}
                        """).param("event", event))
                .call()
                .entity(AuditAnalyzeResponse.class);

        assert response != null;
        return normalize(response);
    }

    public AuditAnalyzeResponse analyzeEventWithHistory(String userId, String event) {
        AuditAnalyzeResponse response = chatClient.prompt()
                .system("""
                        You are an Agentic Audit Security Analyst AI.

                        You may use tools to fetch:
                        1. the user's security profile
                        2. the user's recent events

                        Use the tools when helpful before making a decision.

                        Return ONLY the structured output matching the response schema.

                        CATEGORY MUST BE EXACTLY ONE OF:
                        - suspicious_login
                        - data_access
                        - privilege_change
                        - admin_action
                        - policy_violation
                        - benign
                        - unknown

                        Constraints:
                        - riskScore must be an integer from 0 to 10
                        - summary must be short
                        - reasons must contain 3 to 6 short items
                        - tags must be lowercase with underscores where needed
                        - recommendedAction must be one short sentence

                        Consider unusual geography, odd timing, repeated failures,
                        new devices, privilege escalation, and deviation from past behavior.
                        """)
                .user(u -> u.text("""
                        Analyze this event for user {userId}.

                        CURRENT EVENT:
                        {event}
                        """)
                        .param("userId", userId)
                        .param("event", event))
                .tools(auditTools)
                .call()
                .entity(AuditAnalyzeResponse.class);

        auditHistoryStore.addEvent(userId, event);

        assert response != null;
        return normalize(response);
    }

    private AuditAnalyzeResponse normalize(AuditAnalyzeResponse response) {
        int clampedRiskScore = Math.max(0, Math.min(10, response.riskScore()));

        return new AuditAnalyzeResponse(
                clampedRiskScore,
                response.category(),
                response.summary(),
                response.reasons(),
                response.tags(),
                response.recommendedAction()
        );
    }
}
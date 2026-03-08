package com.agentic.ai.spring_ai_service.service;

import com.agentic.ai.spring_ai_service.audit.dto.response.AuditAnalyzeResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditAgentService {

    private final ChatClient chatClient;

    public AuditAgentService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
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

                        EVENT: {event}
                        """).param("event", event))
                .call()
                .entity(AuditAnalyzeResponse.class);

        return response != null ? normalize(response) : fallbackResponse();
    }

    public AuditAnalyzeResponse analyzeEventWithHistory(String userId, String event) {
        AuditAnalyzeResponse response = chatClient.prompt()
                .system("""
                        You are an Audit Security Analyst AI.

                        Analyze the event using only the provided user id and current event text.
                        Do not assume access to any external history store or tools.

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
                        Analyze this event for user {userId}.

                        CURRENT EVENT: {event}
                        """)
                        .param("userId", userId)
                        .param("event", event))
                .call()
                .entity(AuditAnalyzeResponse.class);

        return response != null ? normalize(response) : fallbackResponse();
    }

    private AuditAnalyzeResponse normalize(AuditAnalyzeResponse response) {
        int clampedRiskScore = Math.max(0, Math.min(10, response.riskScore()));

        String category = response.category() != null ? response.category() : "unknown";
        String summary = response.summary() != null ? response.summary() : "No summary generated.";
        List<String> reasons = response.reasons() != null ? response.reasons() : List.of();
        List<String> tags = response.tags() != null ? response.tags() : List.of();
        String recommendedAction = response.recommendedAction() != null
                ? response.recommendedAction()
                : "Review the event manually.";

        return new AuditAnalyzeResponse(
                clampedRiskScore,
                category,
                summary,
                reasons,
                tags,
                recommendedAction
        );
    }

    private AuditAnalyzeResponse fallbackResponse() {
        return new AuditAnalyzeResponse(
                5,
                "unknown",
                "AI analysis could not be generated.",
                List.of("analysis_unavailable"),
                List.of("fallback"),
                "Review the event manually."
        );
    }
}
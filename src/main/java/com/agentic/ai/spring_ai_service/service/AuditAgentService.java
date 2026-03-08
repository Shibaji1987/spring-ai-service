package com.agentic.ai.spring_ai_service.service;

import com.agentic.ai.spring_ai_service.audit.dto.response.AuditAnalyzeResponse;
import com.agentic.ai.spring_ai_service.audit.model.AuditEvent;
import com.agentic.ai.spring_ai_service.audit.tools.AuditTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditAgentService {

    private static final Logger log = LoggerFactory.getLogger(AuditAgentService.class);

    private final ChatClient chatClient;
    private final AuditEventService auditEventService;
    private final AuditTools auditTools;

    public AuditAgentService(ChatClient.Builder builder,
                             AuditEventService auditEventService,
                             AuditTools auditTools) {
        this.chatClient = builder.build();
        this.auditEventService = auditEventService;
        this.auditTools = auditTools;
    }

    public AuditAnalyzeResponse analyzeEvent(String event) {
        log.info("[AGENT] simple analyzeEvent started");
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

        AuditAnalyzeResponse finalResponse = response != null ? normalize(response) : fallbackResponse();
        log.info("[AGENT] simple analyzeEvent completed category={} riskScore={}",
                finalResponse.category(), finalResponse.riskScore());
        return finalResponse;
    }

    public AuditAnalyzeResponse analyzeEventWithTools(String eventId) {
        AuditEvent event = auditEventService.getEventById(eventId);

        log.info("[AGENT] analyzeEventWithTools started eventId={} actor={}", eventId, event.getActor());
        log.info("[AGENT] GOAL analyze existing audit event using tools and current event context");

        String eventText = buildEventText(event);

        AuditAnalyzeResponse response = chatClient.prompt()
                .system("""
                        You are an Audit Security Analyst AI using a ReAct-style workflow.

                        Your goal is to assess audit risk for the current event.
                        You may use tools to gather supporting context before deciding.

                        Use tools especially for:
                        - recent user activity
                        - previous risky incidents
                        - failed login patterns
                        - high-level activity summary

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
                        Analyze this existing audit event.

                        EVENT ID:
                        {eventId}

                        CURRENT EVENT:
                        {eventText}

                        ACTOR:
                        {actor}

                        Use tools when needed before giving the final result.
                        """)
                        .param("eventId", eventId)
                        .param("eventText", eventText)
                        .param("actor", event.getActor()))
                .tools(auditTools)
                .call()
                .entity(AuditAnalyzeResponse.class);

        AuditAnalyzeResponse finalResponse = response != null ? normalize(response) : fallbackResponse();

        log.info("[AGENT] FINAL RESULT eventId={} category={} riskScore={} summary={}",
                eventId,
                finalResponse.category(),
                finalResponse.riskScore(),
                finalResponse.summary());

        return finalResponse;
    }

    private String buildEventText(AuditEvent event) {
        return "eventType=" + event.getEventType()
                + ", actor=" + event.getActor()
                + ", action=" + event.getAction()
                + ", target=" + event.getTarget()
                + ", status=" + event.getStatus()
                + ", eventTime=" + event.getEventTime()
                + ", metadata=" + event.getMetadata();
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
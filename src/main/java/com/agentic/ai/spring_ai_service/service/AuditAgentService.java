package com.agentic.ai.spring_ai_service.service;

import com.agentic.ai.spring_ai_service.audit.dto.agent.AgentExecutionTrace;
import com.agentic.ai.spring_ai_service.audit.dto.agent.ToolExecutionResult;
import com.agentic.ai.spring_ai_service.audit.dto.response.AuditAnalyzeResponse;
import com.agentic.ai.spring_ai_service.audit.model.AuditAiAnalysis;
import com.agentic.ai.spring_ai_service.audit.model.AuditEvent;
import com.agentic.ai.spring_ai_service.audit.repository.AuditAiAnalysisRepository;
import com.agentic.ai.spring_ai_service.audit.tools.AuditTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuditAgentService {

    private static final Logger log = LoggerFactory.getLogger(AuditAgentService.class);

    private static final int MAX_TOOL_CALLS = 3;

    private final ChatClient chatClient;
    private final AuditEventService auditEventService;
    private final AuditTools auditTools;
    private final AuditAiAnalysisRepository auditAiAnalysisRepository;

    public AuditAgentService(ChatClient.Builder builder,
                             AuditEventService auditEventService,
                             AuditTools auditTools,
                             AuditAiAnalysisRepository auditAiAnalysisRepository) {
        this.chatClient = builder.build();
        this.auditEventService = auditEventService;
        this.auditTools = auditTools;
        this.auditAiAnalysisRepository = auditAiAnalysisRepository;
    }

    public AuditAnalyzeResponse analyzeEventWithTools(String eventId) {
        AuditEvent auditEvent = auditEventService.getEventById(eventId);

        String actor = auditEvent.getActor();
        String eventText = buildEventText(auditEvent);

        AgentExecutionTrace trace = new AgentExecutionTrace();
        trace.setEventId(eventId);
        trace.setActor(actor);
        trace.setMaxToolCalls(MAX_TOOL_CALLS);

        log.info("[AGENT][{}] started eventId={} actor={}", trace.getTraceId(), eventId, actor);

        List<String> contextChunks = new ArrayList<>();

        ToolExecutionResult summaryResult = safeToolCall(
                "getUserActivitySummary",
                trace,
                () -> auditTools.getUserActivitySummary(actor)
        );
        if (summaryResult.success()) {
            contextChunks.add("User activity summary: " + summaryResult.data());
        }

        if (trace.getToolCallsAttempted() < MAX_TOOL_CALLS && isLoginEvent(auditEvent)) {
            ToolExecutionResult failedLoginResult = safeToolCall(
                    "getFailedLoginCount",
                    trace,
                    () -> auditTools.getFailedLoginCount(actor)
            );
            if (failedLoginResult.success()) {
                contextChunks.add("Failed login count: " + failedLoginResult.data());
            }
        }

        if (trace.getToolCallsAttempted() < MAX_TOOL_CALLS && looksSuspicious(auditEvent)) {
            ToolExecutionResult highRiskResult = safeToolCall(
                    "getPreviousHighRiskEvents",
                    trace,
                    () -> auditTools.getPreviousHighRiskEvents(actor, 7)
            );
            if (highRiskResult.success()) {
                contextChunks.add("Previous high risk events: " + highRiskResult.data());
            }
        }

        if (trace.getToolCallsAttempted() < MAX_TOOL_CALLS && needsRecentSequence(auditEvent)) {
            ToolExecutionResult recentEventsResult = safeToolCall(
                    "getRecentEvents",
                    trace,
                    () -> auditTools.getRecentEvents(actor, 5)
            );
            if (recentEventsResult.success()) {
                contextChunks.add("Recent events: " + recentEventsResult.data());
            }
        }

        if (trace.getToolCallsAttempted() >= MAX_TOOL_CALLS) {
            trace.addNote("Tool limit reached.");
            log.warn("[AGENT][{}] bounded stop: maxToolCalls={} reached", trace.getTraceId(), MAX_TOOL_CALLS);
        }

        AuditAnalyzeResponse response = generateFinalAnalysis(eventId, actor, eventText, contextChunks, trace);

        saveAnalysis(eventId, response);

        trace.setStatus(trace.getToolCallsFailed() > 0 ? "PARTIAL_SUCCESS" : "SUCCESS");
        trace.finish();

        log.info(
                "[AGENT][{}] completed status={} attempted={} succeeded={} failed={} toolsUsed={}",
                trace.getTraceId(),
                trace.getStatus(),
                trace.getToolCallsAttempted(),
                trace.getToolCallsSucceeded(),
                trace.getToolCallsFailed(),
                trace.getToolsUsed()
        );

        return response;
    }

    private AuditAnalyzeResponse generateFinalAnalysis(String eventId,
                                                       String actor,
                                                       String eventText,
                                                       List<String> contextChunks,
                                                       AgentExecutionTrace trace) {
        String joinedContext = contextChunks.isEmpty()
                ? "No additional tool context available."
                : String.join("\n", contextChunks);

        AuditAnalyzeResponse response = chatClient.prompt()
                .system("""
                        You are an Audit Security Analyst AI.

                        Analyze the current audit event using the supplied context.
                        Use the extra context only if it is relevant.
                        If some context is missing, still provide the best possible answer.

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
                        EVENT ID: {eventId}
                        ACTOR: {actor}
                        CURRENT EVENT:
                        {eventText}

                        ADDITIONAL CONTEXT:
                        {toolContext}
                        """)
                        .param("eventId", eventId)
                        .param("actor", actor)
                        .param("eventText", eventText)
                        .param("toolContext", joinedContext))
                .call()
                .entity(AuditAnalyzeResponse.class);

        AuditAnalyzeResponse finalResponse = response != null ? normalize(response) : fallbackResponse();

        log.info("[AGENT][{}] final category={} riskScore={} summary={}",
                trace.getTraceId(),
                finalResponse.category(),
                finalResponse.riskScore(),
                finalResponse.summary());

        return finalResponse;
    }

    private ToolExecutionResult safeToolCall(String toolName,
                                             AgentExecutionTrace trace,
                                             ToolSupplier supplier) {
        trace.incrementAttempted();
        trace.addTool(toolName);

        long start = System.currentTimeMillis();
        try {
            log.info("[AGENT][{}] calling tool={}", trace.getTraceId(), toolName);

            Object data = supplier.get();

            long duration = System.currentTimeMillis() - start;
            trace.incrementSucceeded();

            log.info("[AGENT][{}] tool={} success durationMs={}", trace.getTraceId(), toolName, duration);

            return ToolExecutionResult.success(toolName, data, duration);
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - start;
            trace.incrementFailed();
            trace.addNote("Tool failed: " + toolName + " -> " + ex.getMessage());

            log.warn("[AGENT][{}] tool={} failed durationMs={} error={}",
                    trace.getTraceId(), toolName, duration, ex.getMessage());

            return ToolExecutionResult.failure(toolName, ex.getMessage(), duration);
        }
    }

    private void saveAnalysis(String eventId, AuditAnalyzeResponse response) {
        AuditAiAnalysis analysis = auditAiAnalysisRepository.findByAuditEventId(eventId)
                .orElseGet(AuditAiAnalysis::new);

        analysis.setAuditEventId(eventId);
        analysis.setRiskScore(response.riskScore());
        analysis.setCategory(response.category());
        analysis.setSummary(response.summary());
        analysis.setReasons(response.reasons());
        analysis.setTags(response.tags());
        analysis.setRecommendedAction(response.recommendedAction());

        AuditAiAnalysis saved = auditAiAnalysisRepository.save(analysis);

        log.info("[AGENT] analysis saved eventId={} analysisId={} category={} riskScore={}",
                eventId, saved.getId(), saved.getCategory(), saved.getRiskScore());
    }

    private boolean isLoginEvent(AuditEvent event) {
        return event.getEventType() != null && event.getEventType().toUpperCase().contains("LOGIN");
    }

    private boolean looksSuspicious(AuditEvent event) {
        if (event.getStatus() != null && event.getStatus().equalsIgnoreCase("FAILURE")) {
            return true;
        }

        if (event.getAction() != null) {
            String action = event.getAction().toUpperCase();
            return action.contains("PRIVILEGE")
                    || action.contains("DELETE")
                    || action.contains("EXPORT")
                    || action.contains("LOCK")
                    || action.contains("ADMIN");
        }

        return false;
    }

    private boolean needsRecentSequence(AuditEvent event) {
        return looksSuspicious(event) || isLoginEvent(event);
    }

    private String buildEventText(AuditEvent event) {
        return """
                Event Type: %s
                Actor: %s
                Action: %s
                Target: %s
                Status: %s
                Event Time: %s
                Metadata: %s
                """.formatted(
                event.getEventType(),
                event.getActor(),
                event.getAction(),
                event.getTarget(),
                event.getStatus(),
                event.getEventTime(),
                event.getMetadata()
        );
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

    @FunctionalInterface
    private interface ToolSupplier {
        Object get();
    }
}
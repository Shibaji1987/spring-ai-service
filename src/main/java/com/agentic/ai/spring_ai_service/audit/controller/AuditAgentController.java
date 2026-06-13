package com.agentic.ai.spring_ai_service.audit.controller;

import com.agentic.ai.spring_ai_service.audit.dto.response.AuditAnalysisResponseDto;
import com.agentic.ai.spring_ai_service.audit.dto.response.AuditAnalysisStreamEventDto;
import com.agentic.ai.spring_ai_service.service.AuditAgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.function.Consumer;

@RestController
@RequestMapping("/audit")
public class AuditAgentController {

    private static final Logger log = LoggerFactory.getLogger(AuditAgentController.class);
    private static final long ANALYSIS_STREAM_TIMEOUT_MS = 10 * 60 * 1000L;

    private final AuditAgentService auditAgentService;
    private final TaskExecutor taskExecutor;

    public AuditAgentController(AuditAgentService auditAgentService, TaskExecutor taskExecutor) {
        this.auditAgentService = auditAgentService;
        this.taskExecutor = taskExecutor;
    }

    @PostMapping("/analyze-with-tools/{eventId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public AuditAnalysisResponseDto analyzeWithTools(@PathVariable String eventId) {
        log.info("Invoking Agent");
        return auditAgentService.analyzeEventWithTools(eventId);
    }

    @GetMapping(value = "/analyze-with-tools/{eventId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public SseEmitter streamAnalyzeWithTools(@PathVariable String eventId) {
        return streamAnalysis(eventId, progressSink ->
                auditAgentService.analyzeEventWithTools(eventId, progressSink));
    }

    @PostMapping("/analyze-with-llm-tools/{eventId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public AuditAnalysisResponseDto analyzeWithLlmTools(@PathVariable String eventId) {
        return auditAgentService.analyzeEventWithLlmTools(eventId);
    }

    @GetMapping(value = "/analyze-with-llm-tools/{eventId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public SseEmitter streamAnalyzeWithLlmTools(@PathVariable String eventId) {
        return streamAnalysis(
                eventId,
                progressSink -> auditAgentService.analyzeEventWithLlmTools(eventId, progressSink)
        );
    }

    private SseEmitter streamAnalysis(
            String eventId,
            StreamingAnalysis analysis
    ) {
        SseEmitter emitter = new SseEmitter(ANALYSIS_STREAM_TIMEOUT_MS);

        taskExecutor.execute(() -> {
            try {
                analysis.run(event -> sendEvent(emitter, event));
                emitter.complete();
            } catch (Exception ex) {
                log.error("Streaming LLM analysis failed. eventId={} error={}", eventId, ex.getMessage(), ex);
                sendEvent(emitter, failureEvent(eventId, ex));
                emitter.completeWithError(ex);
            }
        });

        return emitter;
    }

    private AuditAnalysisStreamEventDto failureEvent(String eventId, Exception ex) {
        return AuditAnalysisStreamEventDto.builder()
                .eventId(eventId)
                .phase("ANALYSIS_FAILED")
                .status("FAILED")
                .message(ex.getMessage())
                .timestamp(Instant.now())
                .build();
    }

    private void sendEvent(SseEmitter emitter, AuditAnalysisStreamEventDto event) {
        try {
            emitter.send(SseEmitter.event()
                    .name(toSseEventName(event.getPhase()))
                    .data(event));
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to stream audit analysis event.", ex);
        }
    }

    private String toSseEventName(String phase) {
        if (phase == null || phase.isBlank()) {
            return "audit-analysis";
        }

        return phase.toLowerCase().replace('_', '-');
    }

    @FunctionalInterface
    private interface StreamingAnalysis {
        void run(Consumer<AuditAnalysisStreamEventDto> progressSink);
    }
}

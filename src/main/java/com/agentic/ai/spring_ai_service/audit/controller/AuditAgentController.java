package com.agentic.ai.spring_ai_service.audit.controller;

import com.agentic.ai.spring_ai_service.audit.dto.response.AuditAnalysisResponseDto;
import com.agentic.ai.spring_ai_service.audit.dto.response.AuditAnalysisStreamEventDto;
import com.agentic.ai.spring_ai_service.audit.dto.response.AnalysisRunResponse;
import com.agentic.ai.spring_ai_service.audit.dto.response.CreateAnalysisRunResponse;
import com.agentic.ai.spring_ai_service.service.AuditAnalysisRunService;
import com.agentic.ai.spring_ai_service.service.AuditAgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.function.Consumer;

@RestController
@RequestMapping("/audit")
@Tag(
        name = "Audit Analysis",
        description = "Creates and observes bounded LLM-directed audit investigations. Prefer the analysis-run endpoints for new integrations."
)
public class AuditAgentController {

    private static final Logger log = LoggerFactory.getLogger(AuditAgentController.class);
    private static final long ANALYSIS_STREAM_TIMEOUT_MS = 10 * 60 * 1000L;

    private final AuditAgentService auditAgentService;
    private final AuditAnalysisRunService analysisRunService;
    private final TaskExecutor taskExecutor;

    public AuditAgentController(
            AuditAgentService auditAgentService,
            AuditAnalysisRunService analysisRunService,
            TaskExecutor taskExecutor
    ) {
        this.auditAgentService = auditAgentService;
        this.analysisRunService = analysisRunService;
        this.taskExecutor = taskExecutor;
    }

    @Operation(
            summary = "Create an LLM analysis run",
            description = """
                    Starts exactly one asynchronous, persisted analysis run for an existing audit event.
                    The bounded LLM receives the event and retrieved policy evidence, requests allowlisted
                    investigation tools only when needed, consumes their observations, and persists the final result.
                    Use the returned streamUrl to watch progress and resultUrl to poll or retrieve the final state.
                    """,
            responses = {
                    @ApiResponse(responseCode = "202", description = "Analysis run accepted and started",
                            content = @Content(schema = @Schema(implementation = CreateAnalysisRunResponse.class))),
                    @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
                    @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN or ANALYST"),
                    @ApiResponse(responseCode = "404", description = "Audit event does not exist")
            }
    )
    @PostMapping("/events/{eventId}/analysis-runs")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<CreateAnalysisRunResponse> createAnalysisRun(
            @Parameter(description = "MongoDB identifier of the audit event to investigate", required = true)
            @PathVariable String eventId
    ) {
        return ResponseEntity.accepted().body(analysisRunService.create(eventId));
    }

    @Operation(
            summary = "Stream an analysis run",
            description = """
                    Opens a Server-Sent Events stream for one previously created analysis run.
                    Buffered events are replayed first, so clients may connect after the run has started.
                    Live events include policy retrieval, audit-safe LLM rationale summaries, tool requests,
                    tool results, final assessment, and failure information. The stream closes when the run
                    reaches COMPLETED or FAILED. Creating another run is not required.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "SSE stream opened",
                            content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE)),
                    @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
                    @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN or ANALYST"),
                    @ApiResponse(responseCode = "404", description = "Analysis run does not exist")
            }
    )
    @GetMapping(value = "/analysis-runs/{runId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public SseEmitter streamAnalysisRun(
            @Parameter(description = "Identifier returned by the create-analysis-run endpoint", required = true)
            @PathVariable String runId
    ) {
        return analysisRunService.subscribe(runId);
    }

    @Operation(
            summary = "Get analysis run status or result",
            description = """
                    Returns the durable state of an analysis run without starting or repeating analysis.
                    PENDING and RUNNING responses describe current progress. COMPLETED includes the full persisted
                    analysis result. FAILED includes the error message and timestamps. This endpoint is suitable
                    for polling, reconnect recovery, automation, and retrieving the result after SSE completes.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Current analysis run state",
                            content = @Content(schema = @Schema(implementation = AnalysisRunResponse.class))),
                    @ApiResponse(responseCode = "401", description = "JWT token is missing or invalid"),
                    @ApiResponse(responseCode = "403", description = "Caller is not authorized to view analysis"),
                    @ApiResponse(responseCode = "404", description = "Analysis run does not exist")
            }
    )
    @GetMapping("/analysis-runs/{runId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'POLICY_MANAGER', 'ANALYST', 'VIEWER')")
    public AnalysisRunResponse getAnalysisRun(
            @Parameter(description = "Identifier returned by the create-analysis-run endpoint", required = true)
            @PathVariable String runId
    ) {
        return analysisRunService.get(runId);
    }

    @Operation(
            summary = "Run deterministic tool-assisted analysis (legacy)",
            description = "Legacy synchronous endpoint. Java selects tools using deterministic conditions. Prefer POST /audit/events/{eventId}/analysis-runs.",
            deprecated = true
    )
    @Deprecated
    @PostMapping("/analyze-with-tools/{eventId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public AuditAnalysisResponseDto analyzeWithTools(@PathVariable String eventId) {
        log.info("Invoking Agent");
        return auditAgentService.analyzeEventWithTools(eventId);
    }

    @Operation(
            summary = "Stream deterministic tool-assisted analysis (legacy)",
            description = "Legacy endpoint that starts a new deterministic analysis when the stream opens. Prefer the analysis-run lifecycle.",
            deprecated = true
    )
    @Deprecated
    @GetMapping(value = "/analyze-with-tools/{eventId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public SseEmitter streamAnalyzeWithTools(@PathVariable String eventId) {
        return streamAnalysis(eventId, progressSink ->
                auditAgentService.analyzeEventWithTools(eventId, progressSink));
    }

    @Operation(
            summary = "Run LLM-directed analysis synchronously (legacy)",
            description = "Runs a complete LLM-directed investigation and waits for the final response. Prefer creating an analysis run to avoid client timeouts and duplicate execution.",
            deprecated = true
    )
    @Deprecated
    @PostMapping("/analyze-with-llm-tools/{eventId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public AuditAnalysisResponseDto analyzeWithLlmTools(@PathVariable String eventId) {
        return auditAgentService.analyzeEventWithLlmTools(eventId);
    }

    @Operation(
            summary = "Start and stream LLM-directed analysis (legacy)",
            description = "Starts a new analysis as a side effect of opening the stream. Prefer POST /audit/events/{eventId}/analysis-runs followed by GET /audit/analysis-runs/{runId}/stream.",
            deprecated = true
    )
    @Deprecated
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

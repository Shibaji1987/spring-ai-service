package com.agentic.ai.spring_ai_service.service;

import com.agentic.ai.spring_ai_service.audit.dto.response.AnalysisRunResponse;
import com.agentic.ai.spring_ai_service.audit.dto.response.AuditAnalysisResponseDto;
import com.agentic.ai.spring_ai_service.audit.dto.response.AuditAnalysisStreamEventDto;
import com.agentic.ai.spring_ai_service.audit.dto.response.CreateAnalysisRunResponse;
import com.agentic.ai.spring_ai_service.audit.model.AuditAnalysisRun;
import com.agentic.ai.spring_ai_service.audit.repository.AuditAnalysisRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditAnalysisRunService {

    private static final String PENDING = "PENDING";
    private static final String RUNNING = "RUNNING";
    private static final String COMPLETED = "COMPLETED";
    private static final String FAILED = "FAILED";
    private static final long STREAM_TIMEOUT_MS = 10 * 60 * 1000L;

    private final AuditAnalysisRunRepository repository;
    private final AuditEventService auditEventService;
    private final AuditAgentService auditAgentService;
    private final TaskExecutor taskExecutor;

    private final Map<String, Object> runLocks = new ConcurrentHashMap<>();
    private final Map<String, List<SseEmitter>> subscribers = new ConcurrentHashMap<>();

    public CreateAnalysisRunResponse create(String eventId) {
        verifyEventExists(eventId);

        AuditAnalysisRun run = repository.save(AuditAnalysisRun.builder()
                .eventId(eventId)
                .status(PENDING)
                .createdAt(Instant.now())
                .build());

        CreateAnalysisRunResponse response = toCreateResponse(run);
        taskExecutor.execute(() -> execute(run.getId()));
        return response;
    }

    public AnalysisRunResponse get(String runId) {
        return toResponse(findRun(runId));
    }

    public SseEmitter subscribe(String runId) {
        Object lock = lockFor(runId);
        synchronized (lock) {
            AuditAnalysisRun run = findRun(runId);
            SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);

            configureEmitter(runId, emitter);
            replay(run, emitter);

            if (isTerminal(run.getStatus())) {
                emitter.complete();
            } else {
                subscribers.computeIfAbsent(runId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
            }

            return emitter;
        }
    }

    private void execute(String runId) {
        try {
            updateRun(runId, run -> {
                run.setStatus(RUNNING);
                run.setStartedAt(Instant.now());
            });

            AuditAnalysisRun current = findRun(runId);
            AuditAnalysisResponseDto result = auditAgentService.analyzeEventWithLlmTools(
                    current.getEventId(),
                    event -> recordEvent(runId, event)
            );

            updateRun(runId, run -> {
                run.setStatus(COMPLETED);
                run.setResult(result);
                run.setCompletedAt(Instant.now());
            });
            completeSubscribers(runId);
        } catch (Exception ex) {
            log.error("Analysis run failed. runId={} error={}", runId, ex.getMessage(), ex);
            recordEvent(runId, failureEvent(findRun(runId).getEventId(), ex));
            updateRun(runId, run -> {
                run.setStatus(FAILED);
                run.setErrorMessage(ex.getMessage());
                run.setCompletedAt(Instant.now());
            });
            completeSubscribers(runId);
        }
    }

    private void recordEvent(String runId, AuditAnalysisStreamEventDto event) {
        Object lock = lockFor(runId);
        synchronized (lock) {
            AuditAnalysisRun run = findRun(runId);
            List<AuditAnalysisStreamEventDto> events = new ArrayList<>(run.getEvents());
            events.add(event);
            run.setEvents(events);
            repository.save(run);

            List<SseEmitter> activeSubscribers = subscribers.get(runId);
            if (activeSubscribers != null) {
                activeSubscribers.removeIf(emitter -> !send(emitter, event));
            }
        }
    }

    private void replay(AuditAnalysisRun run, SseEmitter emitter) {
        for (AuditAnalysisStreamEventDto event : run.getEvents()) {
            if (!send(emitter, event)) {
                break;
            }
        }
    }

    private boolean send(SseEmitter emitter, AuditAnalysisStreamEventDto event) {
        try {
            emitter.send(SseEmitter.event()
                    .name(toSseEventName(event.getPhase()))
                    .data(event));
            return true;
        } catch (IOException | IllegalStateException ex) {
            emitter.complete();
            return false;
        }
    }

    private void completeSubscribers(String runId) {
        Object lock = lockFor(runId);
        synchronized (lock) {
            List<SseEmitter> runSubscribers = subscribers.remove(runId);
            if (runSubscribers != null) {
                runSubscribers.forEach(SseEmitter::complete);
            }
            runLocks.remove(runId);
        }
    }

    private void configureEmitter(String runId, SseEmitter emitter) {
        Runnable cleanup = () -> removeSubscriber(runId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ignored -> cleanup.run());
    }

    private void removeSubscriber(String runId, SseEmitter emitter) {
        List<SseEmitter> runSubscribers = subscribers.get(runId);
        if (runSubscribers != null) {
            runSubscribers.remove(emitter);
        }
    }

    private void updateRun(String runId, java.util.function.Consumer<AuditAnalysisRun> update) {
        synchronized (lockFor(runId)) {
            AuditAnalysisRun run = findRun(runId);
            update.accept(run);
            repository.save(run);
        }
    }

    private AuditAnalysisRun findRun(String runId) {
        return repository.findById(runId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Analysis run not found: " + runId
                ));
    }

    private void verifyEventExists(String eventId) {
        try {
            auditEventService.getEventById(eventId);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Audit event not found: " + eventId,
                    ex
            );
        }
    }

    private Object lockFor(String runId) {
        return runLocks.computeIfAbsent(runId, ignored -> new Object());
    }

    private boolean isTerminal(String status) {
        return COMPLETED.equals(status) || FAILED.equals(status);
    }

    private AuditAnalysisStreamEventDto failureEvent(String eventId, Exception ex) {
        return AuditAnalysisStreamEventDto.builder()
                .eventId(eventId)
                .phase("ANALYSIS_FAILED")
                .status(FAILED)
                .message(ex.getMessage())
                .timestamp(Instant.now())
                .build();
    }

    private CreateAnalysisRunResponse toCreateResponse(AuditAnalysisRun run) {
        return new CreateAnalysisRunResponse(
                run.getId(),
                run.getEventId(),
                run.getStatus(),
                run.getCreatedAt(),
                streamUrl(run.getId()),
                resultUrl(run.getId())
        );
    }

    private AnalysisRunResponse toResponse(AuditAnalysisRun run) {
        return new AnalysisRunResponse(
                run.getId(),
                run.getEventId(),
                run.getStatus(),
                run.getCreatedAt(),
                run.getStartedAt(),
                run.getCompletedAt(),
                run.getErrorMessage(),
                run.getResult(),
                streamUrl(run.getId()),
                resultUrl(run.getId())
        );
    }

    private String streamUrl(String runId) {
        return "/audit/analysis-runs/" + runId + "/stream";
    }

    private String resultUrl(String runId) {
        return "/audit/analysis-runs/" + runId;
    }

    private String toSseEventName(String phase) {
        return phase == null || phase.isBlank()
                ? "audit-analysis"
                : phase.toLowerCase().replace('_', '-');
    }
}

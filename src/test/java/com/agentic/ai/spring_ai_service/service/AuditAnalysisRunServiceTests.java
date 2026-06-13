package com.agentic.ai.spring_ai_service.service;

import com.agentic.ai.spring_ai_service.audit.dto.response.AnalysisRunResponse;
import com.agentic.ai.spring_ai_service.audit.dto.response.AuditAnalysisResponseDto;
import com.agentic.ai.spring_ai_service.audit.dto.response.AuditAnalysisStreamEventDto;
import com.agentic.ai.spring_ai_service.audit.dto.response.CreateAnalysisRunResponse;
import com.agentic.ai.spring_ai_service.audit.model.AuditAnalysisRun;
import com.agentic.ai.spring_ai_service.audit.model.AuditEvent;
import com.agentic.ai.spring_ai_service.audit.repository.AuditAnalysisRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.SyncTaskExecutor;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuditAnalysisRunServiceTests {

    private final AuditAnalysisRunRepository repository = mock(AuditAnalysisRunRepository.class);
    private final AuditEventService auditEventService = mock(AuditEventService.class);
    private final AuditAgentService auditAgentService = mock(AuditAgentService.class);
    private final Map<String, AuditAnalysisRun> storedRuns = new ConcurrentHashMap<>();

    private AuditAnalysisRunService service;

    @BeforeEach
    void setUp() {
        when(repository.save(any(AuditAnalysisRun.class))).thenAnswer(invocation -> {
            AuditAnalysisRun run = invocation.getArgument(0);
            if (run.getId() == null) {
                run.setId(UUID.randomUUID().toString());
            }
            storedRuns.put(run.getId(), run);
            return run;
        });
        when(repository.findById(any(String.class)))
                .thenAnswer(invocation -> Optional.ofNullable(storedRuns.get(invocation.getArgument(0))));
        when(auditEventService.getEventById("event-1")).thenReturn(new AuditEvent());

        when(auditAgentService.analyzeEventWithLlmTools(eq("event-1"), any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Consumer<AuditAnalysisStreamEventDto> sink = invocation.getArgument(1);
                    sink.accept(AuditAnalysisStreamEventDto.builder()
                            .eventId("event-1")
                            .phase("LLM_DECISION")
                            .status("COMPLETED")
                            .message("Enough evidence is available.")
                            .timestamp(Instant.now())
                            .build());
                    return AuditAnalysisResponseDto.builder()
                            .eventId("event-1")
                            .riskScore(3)
                            .category("LOW_RISK")
                            .build();
                });

        service = new AuditAnalysisRunService(
                repository,
                auditEventService,
                auditAgentService,
                new SyncTaskExecutor()
        );
    }

    @Test
    void createsOneRunAndExposesDurableStatusStreamAndResultUrls() {
        CreateAnalysisRunResponse created = service.create("event-1");
        AnalysisRunResponse completed = service.get(created.analysisRunId());

        assertThat(created.status()).isEqualTo("PENDING");
        assertThat(created.streamUrl())
                .isEqualTo("/audit/analysis-runs/" + created.analysisRunId() + "/stream");
        assertThat(created.resultUrl())
                .isEqualTo("/audit/analysis-runs/" + created.analysisRunId());

        assertThat(completed.status()).isEqualTo("COMPLETED");
        assertThat(completed.result().getCategory()).isEqualTo("LOW_RISK");
        assertThat(storedRuns.get(created.analysisRunId()).getEvents())
                .extracting(AuditAnalysisStreamEventDto::getPhase)
                .containsExactly("LLM_DECISION");
    }
}

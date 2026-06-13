package com.agentic.ai.spring_ai_service.audit.model;

import com.agentic.ai.spring_ai_service.audit.dto.response.AuditAnalysisResponseDto;
import com.agentic.ai.spring_ai_service.audit.dto.response.AuditAnalysisStreamEventDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document("audit_analysis_runs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditAnalysisRun {

    @Id
    private String id;

    private String eventId;
    private String status;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
    private String errorMessage;
    private AuditAnalysisResponseDto result;

    @Builder.Default
    private List<AuditAnalysisStreamEventDto> events = new ArrayList<>();
}

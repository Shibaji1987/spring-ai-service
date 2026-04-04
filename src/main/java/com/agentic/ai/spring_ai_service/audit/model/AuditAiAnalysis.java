package com.agentic.ai.spring_ai_service.audit.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Document("audit_ai_analysis")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditAiAnalysis {

    @Id
    private String id;

    private String eventId;

    private Integer riskScore;              // 0-100
    private String category;                // benign, suspicious, high_risk, critical
    private Double confidenceScore;         // 0.0 - 1.0
    private String confidenceLabel;         // LOW, MEDIUM, HIGH

    private String summary;
    private List<String> reasons;
    private List<String> tags;
    private String recommendedAction;

    private Boolean grounded;
    private Boolean fallbackUsed;
    private Boolean toolsInvoked;
    private Boolean analysisSucceeded;

    private String modelName;
    private String analysisVersion;         // e.g. v2.0.0
    private Instant analyzedAt;

    private List<MatchedPolicyEvidence> matchedPolicyEvidence;
    private List<ToolExecutionRecord> toolExecutions;

    private AnalysisDiagnostics diagnostics;

    private Map<String, Object> rawStructuredResponse; // optional for debugging
}
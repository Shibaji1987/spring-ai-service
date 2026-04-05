package com.agentic.ai.spring_ai_service.audit.model;

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

    // Core analysis
    private Integer riskScore;          // recommended scale: 0-10
    private String category;            // BENIGN / SUSPICIOUS_LOGIN / UNUSUAL_ADMIN_ACTION / REVIEW_REQUIRED
    private String summary;
    @Builder.Default
    private List<String> reasons = new ArrayList<>();
    @Builder.Default
    private List<String> tags = new ArrayList<>();
    private String recommendedAction;

    // Confidence
    private Double confidenceScore;     // 0.0 - 1.0
    private String confidenceLabel;     // LOW / MEDIUM / HIGH

    // Analysis metadata
    private Boolean grounded;
    private Boolean fallbackUsed;
    private Boolean toolsInvoked;
    private Boolean analysisSucceeded;

    private String modelName;
    private String analysisVersion;
    private Instant analyzedAt;

    // Evidence + execution trace
    @Builder.Default
    private List<MatchedPolicyEvidence> matchedPolicyEvidence = new ArrayList<>();

    @Builder.Default
    private List<ToolExecutionRecord> toolExecutions = new ArrayList<>();

    private AnalysisDiagnostics diagnostics;

    @Builder.Default
    private List<ReasoningStep> reasoningTrace = new ArrayList<>();

    // Optional debug payload from structured LLM response
    private Map<String, Object> rawStructuredResponse;
}
package com.agentic.ai.spring_ai_service.dto.audit;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonPropertyOrder({ "riskScore", "category", "summary", "reasons", "tags", "recommendedAction" })
public record AuditAnalyzeResponse(
        int riskScore,                 // 0..10
        String category,               // e.g. suspicious_login, data_access, admin_action, benign, unknown
        String summary,                // 1-2 lines
        List<String> reasons,          // short bullet reasons
        List<String> tags,             // keywords
        String recommendedAction       // what to do next
) {}
package com.agentic.ai.spring_ai_service.audit.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record AuditAnalyzeRequest(
        String eventType,
        String actor,
        String action,
        String target,
        String status,
        LocalDateTime eventTime,
        Map<String, Object> metadata
) {}
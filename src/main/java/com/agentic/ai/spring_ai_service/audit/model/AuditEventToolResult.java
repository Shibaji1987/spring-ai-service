package com.agentic.ai.spring_ai_service.audit.model;

import java.time.Instant;

public record AuditEventToolResult(
        String eventId,
        String eventType,
        String status,
        String ipAddress,
        String location,
        Instant eventTime
) {}
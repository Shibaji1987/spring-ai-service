package com.agentic.ai.spring_ai_service.audit.tools;

import com.agentic.ai.spring_ai_service.audit.model.AuditEvent;
import com.agentic.ai.spring_ai_service.audit.model.AuditEventToolResult;
import com.agentic.ai.spring_ai_service.audit.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AuditInvestigationTools {

    private final AuditEventRepository auditEventRepository;

    @Tool(description = """
        Find recent audit events for a given user within the last N hours.
        Use this only when additional investigation is required to verify suspicious activity.
        """)
    public List<AuditEventToolResult> findRecentEventsByUser(String userId, Integer hours) {

        int lookbackHours = (hours == null || hours <= 0) ? 24 : hours;

        var from = java.time.LocalDateTime.now().minusHours(lookbackHours);

        return auditEventRepository
                .findByActorAndEventTimeAfterOrderByEventTimeDesc(userId, from)
                .stream()
                .limit(20)
                .map(this::toToolResult)
                .toList();
    }

    private AuditEventToolResult toToolResult(AuditEvent event) {
        return new AuditEventToolResult(
                event.getId(),
                event.getEventType(),
                event.getStatus(),
                extractIpAddress(event),
                extractLocation(event),
                event.getEventTime() == null
                        ? null
                        : event.getEventTime().toInstant(ZoneOffset.UTC)
        );
    }

    private String extractIpAddress(AuditEvent event) {
        if (event.getMetadata() == null) {
            return null;
        }

        Object value = event.getMetadata().get("ipAddress");
        if (value == null) value = event.getMetadata().get("ip");
        if (value == null) value = event.getMetadata().get("sourceIp");

        return value == null ? null : String.valueOf(value);
    }

    private String extractLocation(AuditEvent event) {
        if (event.getMetadata() == null) {
            return null;
        }

        Object value = event.getMetadata().get("location");
        if (value == null) value = event.getMetadata().get("geoLocation");
        if (value == null) value = event.getMetadata().get("city");

        return value == null ? null : String.valueOf(value);
    }
}
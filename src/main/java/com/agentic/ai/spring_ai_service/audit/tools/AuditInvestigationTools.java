package com.agentic.ai.spring_ai_service.audit.tools;


import com.agentic.ai.spring_ai_service.audit.model.AuditEventToolResult;
import com.agentic.ai.spring_ai_service.audit.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.Instant;
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
        Instant from = Instant.now().minusSeconds((hours == null ? 24 : hours) * 3600L);

        return auditEventRepository
                .findByUserIdAndEventTimeAfterOrderByEventTimeDesc(userId, from)
                .stream()
                .limit(20)
                .map(e -> new AuditEventToolResult(
                        e.getId(),
                        e.getEventType(),
                        e.getStatus(),
                        e.getIpAddress(),
                        e.getLocation(),
                        e.getEventTime()));
    }
}
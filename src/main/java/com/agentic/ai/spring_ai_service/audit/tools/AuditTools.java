package com.agentic.ai.spring_ai_service.audit.tools;


import com.agentic.ai.spring_ai_service.audit.dto.AuditHistoryStore;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AuditTools {

    private final AuditHistoryStore auditHistoryStore;

    public AuditTools(AuditHistoryStore auditHistoryStore) {
        this.auditHistoryStore = auditHistoryStore;
    }

    @Tool(description = "Get the security profile of a user including role, usual country, usual city, MFA status, and login pattern.")
    public String getUserProfile(
            @ToolParam(description = "The user id to lookup") String userId
    ) {
        return auditHistoryStore.userProfile(userId);
    }

    @Tool(description = "Get recent audit events for a user for risk analysis context.")
    public List<AuditHistoryStore.StoredEvent> getLastEvents(
            @ToolParam(description = "The user id to lookup") String userId,
            @ToolParam(description = "Number of recent events to fetch between 1 and 10") int limit
    ) {
        return auditHistoryStore.lastEvents(userId, limit);
    }
}
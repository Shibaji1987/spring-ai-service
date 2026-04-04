package com.agentic.ai.spring_ai_service.audit.tools;

import com.agentic.ai.spring_ai_service.audit.model.AuditEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditContext {

    private String eventId;
    private String actor;
    private String eventType;
    private String status;
    private AuditEvent auditEvent;

    public static AuditContext from(AuditEvent event) {
        if (event == null) {
            return AuditContext.builder().build();
        }

        return AuditContext.builder()
                .eventId(event.getId())
                .actor(event.getActor())
                .eventType(event.getEventType())
                .status(event.getStatus())
                .auditEvent(event)
                .build();
    }
}
package com.agentic.ai.spring_ai_service.audit.controller;


import com.agentic.ai.spring_ai_service.audit.dto.request.AuditEventRequest;
import com.agentic.ai.spring_ai_service.audit.model.AuditEvent;
import com.agentic.ai.spring_ai_service.service.AuditEventService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/audit")
public class AuditEventController {

    private final AuditEventService auditEventService;

    public AuditEventController(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    @PostMapping("/event")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public AuditEvent createAuditEvent(@RequestBody AuditEventRequest request) {
        return auditEventService.saveAuditEvent(request);
    }

    @GetMapping("/events")
    @PreAuthorize("hasAnyRole('ADMIN', 'POLICY_MANAGER', 'ANALYST', 'VIEWER')")
    public java.util.List<AuditEvent> getAllEvents() {
        return auditEventService.getAllEvents();
    }

    @GetMapping("/events/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'POLICY_MANAGER', 'ANALYST', 'VIEWER')")
    public AuditEvent getEventById(@PathVariable String id) {
        return auditEventService.getEventById(id);
    }

    @GetMapping("/events/actor/{actor}")
    @PreAuthorize("hasAnyRole('ADMIN', 'POLICY_MANAGER', 'ANALYST', 'VIEWER')")
    public java.util.List<AuditEvent> getEventsByActor(@PathVariable String actor) {
        return auditEventService.getEventsByActor(actor);
    }

    @GetMapping("/events/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'POLICY_MANAGER', 'ANALYST', 'VIEWER')")
    public java.util.List<AuditEvent> getEventsByStatus(@PathVariable String status) {
        return auditEventService.getEventsByStatus(status);
    }

    @GetMapping("/events/type/{eventType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'POLICY_MANAGER', 'ANALYST', 'VIEWER')")
    public java.util.List<AuditEvent> getEventsByEventType(@PathVariable String eventType) {
        return auditEventService.getEventsByEventType(eventType);
    }
}

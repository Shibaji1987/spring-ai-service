package com.agentic.ai.spring_ai_service.controller;


import com.agentic.ai.spring_ai_service.audit.dto.AuditEventRequest;
import com.agentic.ai.spring_ai_service.audit.model.AuditEvent;
import com.agentic.ai.spring_ai_service.service.AuditEventService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/audit")
public class AuditEventController {

    private final AuditEventService auditEventService;

    public AuditEventController(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    @PostMapping("/event")
    public AuditEvent createAuditEvent(@RequestBody AuditEventRequest request) {
        return auditEventService.saveAuditEvent(request);
    }

    @GetMapping("/events")
    public java.util.List<AuditEvent> getAllEvents() {
        return auditEventService.getAllEvents();
    }

    @GetMapping("/events/{id}")
    public AuditEvent getEventById(@PathVariable String id) {
        return auditEventService.getEventById(id);
    }

    @GetMapping("/events/actor/{actor}")
    public java.util.List<AuditEvent> getEventsByActor(@PathVariable String actor) {
        return auditEventService.getEventsByActor(actor);
    }

    @GetMapping("/events/status/{status}")
    public java.util.List<AuditEvent> getEventsByStatus(@PathVariable String status) {
        return auditEventService.getEventsByStatus(status);
    }

    @GetMapping("/events/type/{eventType}")
    public java.util.List<AuditEvent> getEventsByEventType(@PathVariable String eventType) {
        return auditEventService.getEventsByEventType(eventType);
    }
}
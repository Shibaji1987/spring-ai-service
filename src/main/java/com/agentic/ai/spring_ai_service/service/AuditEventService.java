package com.agentic.ai.spring_ai_service.service;

import com.agentic.ai.spring_ai_service.audit.dto.AuditEventRequest;
import com.agentic.ai.spring_ai_service.audit.model.AuditEvent;
import com.agentic.ai.spring_ai_service.audit.dto.AuditAnalyzeRequest;
import com.agentic.ai.spring_ai_service.repository.AuditEventRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AuditEventService {

    private final AuditEventRepository auditEventRepository;

    public AuditEventService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    public AuditEvent save(AuditAnalyzeRequest request) {

        AuditEvent event = new AuditEvent();

        event.setEventType(request.eventType());
        event.setActor(request.actor());
        event.setAction(request.action());
        event.setTarget(request.target());
        event.setStatus(request.status());

        event.setEventTime(LocalDateTime.now()
        );

        event.setMetadata(
                request.metadata() != null
                        ? request.metadata()
                        : java.util.Map.of()
        );

        return auditEventRepository.save(event);
    }

    // CREATE EVENT
    public AuditEvent saveAuditEvent(AuditEventRequest request) {

        AuditEvent event = new AuditEvent();

        event.setEventType(request.getEventType());
        event.setActor(request.getActor());
        event.setAction(request.getAction());
        event.setTarget(request.getTarget());
        event.setStatus(request.getStatus());

        event.setEventTime(
               LocalDateTime.now()
        );

        event.setMetadata(
                request.getMetadata() != null
                        ? request.getMetadata()
                        : Map.of()
        );

        return auditEventRepository.save(event);
    }

    // GET ALL EVENTS
    public List<AuditEvent> getAllEvents() {
        return auditEventRepository.findAll();
    }

    // GET BY ID
    public AuditEvent getEventById(String id) {
        return auditEventRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Audit event not found with id: " + id));
    }

    // FILTER BY ACTOR
    public List<AuditEvent> getEventsByActor(String actor) {
        return auditEventRepository.findByActor(actor);
    }

    // FILTER BY STATUS
    public List<AuditEvent> getEventsByStatus(String status) {
        return auditEventRepository.findByStatus(status);
    }

    // FILTER BY EVENT TYPE
    public List<AuditEvent> getEventsByEventType(String eventType) {
        return auditEventRepository.findByEventType(eventType);
    }
}
package com.agentic.ai.spring_ai_service.service;


import com.agentic.ai.spring_ai_service.audit.dto.AuditEventRequest;
import com.agentic.ai.spring_ai_service.audit.model.AuditEvent;

import com.agentic.ai.spring_ai_service.repository.AuditEventRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditEventService {

    private final AuditEventRepository auditEventRepository;

    public AuditEventService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    public AuditEvent saveAuditEvent(AuditEventRequest request) {
        AuditEvent event = new AuditEvent();
        event.setEventType(request.getEventType());
        event.setActor(request.getActor());
        event.setAction(request.getAction());
        event.setTarget(request.getTarget());
        event.setStatus(request.getStatus());
        event.setMetadata(request.getMetadata());
        event.setEventTime(LocalDateTime.now());
        return auditEventRepository.save(event);
    }
    public List<AuditEvent> getAllEvents() {
        return auditEventRepository.findAll();
    }

    public AuditEvent getEventById(String id) {
        return auditEventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Audit event not found: " + id));
    }

    public List<AuditEvent> getEventsByActor(String actor) {
        return auditEventRepository.findByActor(actor);
    }

    public List<AuditEvent> getEventsByStatus(String status) {
        return auditEventRepository.findByStatus(status);
    }

    public List<AuditEvent> getEventsByEventType(String eventType) {
        return auditEventRepository.findByEventType(eventType);
    }
}
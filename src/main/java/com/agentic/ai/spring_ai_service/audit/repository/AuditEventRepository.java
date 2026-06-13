package com.agentic.ai.spring_ai_service.audit.repository;

import com.agentic.ai.spring_ai_service.audit.model.AuditEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;

public interface AuditEventRepository extends MongoRepository<AuditEvent, String> {

    List<AuditEvent> findByActor(String actor);

    List<AuditEvent> findByStatus(String status);

    List<AuditEvent> findByEventType(String eventType);

    long countByEventTimeBetween(LocalDateTime start, LocalDateTime end);

    List<AuditEvent> findByEventTimeBetween(LocalDateTime start, LocalDateTime end);

    List<AuditEvent> findAllByOrderByEventTimeDesc(Pageable pageable);

    List<AuditEvent> findByActorAndEventTimeAfterOrderByEventTimeDesc(
            String actor,
            LocalDateTime eventTime
    );
}

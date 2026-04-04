package com.agentic.ai.spring_ai_service.audit.repository;

import com.agentic.ai.spring_ai_service.audit.model.AuditEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface AuditEventRepository extends MongoRepository<AuditEvent, String> {
    List<AuditEvent> findByActor(String actor);
    List<AuditEvent> findByStatus(String status);
    List<AuditEvent> findByEventType(String eventType);
    List<AuditEvent> findByUserIdAndEventTimeAfterOrderByEventTimeDesc(String userId, Instant eventTime);
}
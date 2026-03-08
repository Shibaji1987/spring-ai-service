package com.agentic.ai.spring_ai_service.repository;


import com.agentic.ai.spring_ai_service.audit.model.AuditAiAnalysis;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AuditAiAnalysisRepository extends MongoRepository<AuditAiAnalysis, String> {

    Optional<AuditAiAnalysis> findByAuditEventId(String auditEventId);
    List<AuditAiAnalysis> findByCategory(String category);

}
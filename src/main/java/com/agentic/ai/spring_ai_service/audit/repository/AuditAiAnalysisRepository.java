package com.agentic.ai.spring_ai_service.audit.repository;


import com.agentic.ai.spring_ai_service.audit.model.AuditAiAnalysis;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface AuditAiAnalysisRepository extends MongoRepository<AuditAiAnalysis, String> {

    Optional<AuditAiAnalysis> findByEventId(String auditEventId);
    List<AuditAiAnalysis> findByCategory(String category);
    List<AuditAiAnalysis> findByEventIdIn(Collection<String> eventIds);

    Optional<AuditAiAnalysis> findFirstByOrderByAnalyzedAtDesc();

}

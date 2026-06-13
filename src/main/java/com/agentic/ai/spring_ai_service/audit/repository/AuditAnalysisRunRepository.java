package com.agentic.ai.spring_ai_service.audit.repository;

import com.agentic.ai.spring_ai_service.audit.model.AuditAnalysisRun;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditAnalysisRunRepository extends MongoRepository<AuditAnalysisRun, String> {
}

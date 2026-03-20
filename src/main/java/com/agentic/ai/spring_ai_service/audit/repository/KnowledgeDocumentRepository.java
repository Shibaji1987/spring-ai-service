package com.agentic.ai.spring_ai_service.audit.repository;

import com.agentic.ai.spring_ai_service.audit.model.KnowledgeDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface KnowledgeDocumentRepository extends MongoRepository<KnowledgeDocument, String> {

    List<KnowledgeDocument> findBySourceType(String sourceType);

    List<KnowledgeDocument> findByTitleContainingIgnoreCase(String title);
}
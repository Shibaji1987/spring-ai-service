package com.agentic.ai.spring_ai_service.audit.repository;

import com.agentic.ai.spring_ai_service.audit.model.KnowledgeChunk;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface KnowledgeChunkRepository extends MongoRepository<KnowledgeChunk, String> {

    List<KnowledgeChunk> findByDocumentId(String documentId);

    List<KnowledgeChunk> findByDocumentTitleContainingIgnoreCase(String documentTitle);
}
package com.agentic.ai.spring_ai_service.service;

import com.agentic.ai.spring_ai_service.audit.model.KnowledgeChunk;
import com.agentic.ai.spring_ai_service.audit.model.KnowledgeDocument;
import com.agentic.ai.spring_ai_service.audit.repository.KnowledgeChunkRepository;
import com.agentic.ai.spring_ai_service.audit.repository.KnowledgeDocumentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KnowledgeIngestionService {

    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final KnowledgeChunkingService knowledgeChunkingService;
    private final KnowledgeEmbeddingService knowledgeEmbeddingService;

    public KnowledgeIngestionService(KnowledgeDocumentRepository knowledgeDocumentRepository,
                                     KnowledgeChunkRepository knowledgeChunkRepository,
                                     KnowledgeChunkingService knowledgeChunkingService,
                                     KnowledgeEmbeddingService knowledgeEmbeddingService) {
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.knowledgeChunkingService = knowledgeChunkingService;
        this.knowledgeEmbeddingService = knowledgeEmbeddingService;
    }

    public KnowledgeDocument ingestDocument(KnowledgeDocument document) {
        validateDocument(document);

        document.setCreatedAt(LocalDateTime.now());
        KnowledgeDocument savedDocument = knowledgeDocumentRepository.save(document);

        List<String> chunks = knowledgeChunkingService.chunk(savedDocument.getContent());
        List<KnowledgeChunk> chunkEntities = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            String chunkText = chunks.get(i);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("sourceType", savedDocument.getSourceType());
            metadata.put("chunkSize", chunkText.length());

            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setDocumentId(savedDocument.getId());
            chunk.setDocumentTitle(savedDocument.getTitle());
            chunk.setChunkIndex(i);
            chunk.setText(chunkText);
            chunk.setEmbedding(knowledgeEmbeddingService.embed(chunkText));
            chunk.setMetadata(metadata);
            chunk.setCreatedAt(LocalDateTime.now());

            chunkEntities.add(chunk);
        }

        knowledgeChunkRepository.saveAll(chunkEntities);
        return savedDocument;
    }

    private void validateDocument(KnowledgeDocument document) {
        if (document == null) {
            throw new IllegalArgumentException("KnowledgeDocument cannot be null.");
        }
        if (document.getTitle() == null || document.getTitle().isBlank()) {
            throw new IllegalArgumentException("Document title cannot be blank.");
        }
        if (document.getContent() == null || document.getContent().isBlank()) {
            throw new IllegalArgumentException("Document content cannot be blank.");
        }
    }
}
package com.agentic.ai.spring_ai_service.service;

import com.agentic.ai.spring_ai_service.audit.model.KnowledgeChunk;
import com.agentic.ai.spring_ai_service.audit.repository.KnowledgeChunkRepository;
import com.agentic.ai.spring_ai_service.util.VectorMathUtil;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class KnowledgeRetrievalService {

    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final KnowledgeEmbeddingService knowledgeEmbeddingService;

    public KnowledgeRetrievalService(KnowledgeChunkRepository knowledgeChunkRepository,
                                     KnowledgeEmbeddingService knowledgeEmbeddingService) {
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.knowledgeEmbeddingService = knowledgeEmbeddingService;
    }

    public List<KnowledgeChunk> findTopKRelevantChunks(String query, int topK) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query cannot be null or blank.");
        }

        if (topK <= 0) {
            throw new IllegalArgumentException("topK must be greater than 0.");
        }

        List<Double> queryEmbedding = knowledgeEmbeddingService.embed(query);
        List<KnowledgeChunk> chunks = knowledgeChunkRepository.findAll();

        return chunks.stream()
                .filter(chunk -> chunk.getEmbedding() != null && !chunk.getEmbedding().isEmpty())
                .sorted(Comparator.comparingDouble(
                        (KnowledgeChunk chunk) -> VectorMathUtil.cosineSimilarity(
                                queryEmbedding,
                                chunk.getEmbedding()
                        )
                ).reversed())
                .limit(topK)
                .collect(Collectors.toList());
    }
}
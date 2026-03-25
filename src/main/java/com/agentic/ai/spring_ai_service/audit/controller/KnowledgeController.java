package com.agentic.ai.spring_ai_service.audit.controller;

import com.agentic.ai.spring_ai_service.audit.dto.request.KnowledgeDocumentRequest;
import com.agentic.ai.spring_ai_service.audit.dto.response.KnowledgeSearchResponse;
import com.agentic.ai.spring_ai_service.audit.model.KnowledgeChunk;
import com.agentic.ai.spring_ai_service.audit.model.KnowledgeDocument;
import com.agentic.ai.spring_ai_service.service.KnowledgeIngestionService;
import com.agentic.ai.spring_ai_service.service.KnowledgeRetrievalService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/knowledge")
public class KnowledgeController {

    private final KnowledgeIngestionService knowledgeIngestionService;
    private final KnowledgeRetrievalService knowledgeRetrievalService;

    public KnowledgeController(KnowledgeIngestionService knowledgeIngestionService,
                               KnowledgeRetrievalService knowledgeRetrievalService) {
        this.knowledgeIngestionService = knowledgeIngestionService;
        this.knowledgeRetrievalService = knowledgeRetrievalService;
    }

    @PostMapping("/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> ingestDocument(@RequestBody KnowledgeDocumentRequest request) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setTitle(request.title());
        document.setSourceType(request.sourceType());
        document.setContent(request.content());
        document.setTags(request.tags());
        document.setMetadata(request.metadata());

        KnowledgeDocument saved = knowledgeIngestionService.ingestDocument(document);

        return Map.of(
                "message", "Knowledge document ingested successfully",
                "documentId", saved.getId(),
                "title", saved.getTitle()
        );
    }

    @GetMapping("/search")
    public List<KnowledgeSearchResponse> searchKnowledge(
            @RequestParam("query") String query,
            @RequestParam(value = "topK", defaultValue = "3") int topK) {

        List<KnowledgeChunk> chunks = knowledgeRetrievalService.findTopKRelevantChunks(query, topK);

        return chunks.stream()
                .map(chunk -> new KnowledgeSearchResponse(
                        chunk.getDocumentId(),
                        chunk.getDocumentTitle(),
                        chunk.getChunkIndex(),
                        chunk.getText()
                ))
                .toList();
    }
}
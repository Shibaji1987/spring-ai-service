package com.agentic.ai.spring_ai_service.audit.controller;

import com.agentic.ai.spring_ai_service.audit.dto.request.KnowledgeDocumentRequest;
import com.agentic.ai.spring_ai_service.audit.dto.response.KnowledgeDocumentPageResponse;
import com.agentic.ai.spring_ai_service.audit.dto.response.KnowledgeSearchResponse;
import com.agentic.ai.spring_ai_service.audit.model.KnowledgeChunk;
import com.agentic.ai.spring_ai_service.audit.model.KnowledgeDocument;
import com.agentic.ai.spring_ai_service.service.KnowledgeCatalogService;
import com.agentic.ai.spring_ai_service.service.KnowledgeIngestionService;
import com.agentic.ai.spring_ai_service.service.KnowledgeRetrievalService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/knowledge")
public class KnowledgeController {

    private final KnowledgeCatalogService knowledgeCatalogService;
    private final KnowledgeIngestionService knowledgeIngestionService;
    private final KnowledgeRetrievalService knowledgeRetrievalService;

    public KnowledgeController(KnowledgeCatalogService knowledgeCatalogService,
                               KnowledgeIngestionService knowledgeIngestionService,
                               KnowledgeRetrievalService knowledgeRetrievalService) {
        this.knowledgeCatalogService = knowledgeCatalogService;
        this.knowledgeIngestionService = knowledgeIngestionService;
        this.knowledgeRetrievalService = knowledgeRetrievalService;
    }

    @GetMapping("/documents")
    public KnowledgeDocumentPageResponse getDocuments(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "") String sourceType
    ) {
        return knowledgeCatalogService.getDocuments(page, size, query, sourceType);
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

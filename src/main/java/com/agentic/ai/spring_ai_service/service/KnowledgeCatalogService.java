package com.agentic.ai.spring_ai_service.service;

import com.agentic.ai.spring_ai_service.audit.dto.response.KnowledgeDocumentPageResponse;
import com.agentic.ai.spring_ai_service.audit.dto.response.KnowledgeDocumentSummaryResponse;
import com.agentic.ai.spring_ai_service.audit.model.KnowledgeDocument;
import com.agentic.ai.spring_ai_service.audit.repository.KnowledgeDocumentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class KnowledgeCatalogService {

    private final KnowledgeDocumentRepository knowledgeDocumentRepository;

    public KnowledgeCatalogService(KnowledgeDocumentRepository knowledgeDocumentRepository) {
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
    }

    public KnowledgeDocumentPageResponse getDocuments(
            int page,
            int size,
            String query,
            String sourceType
    ) {
        String normalizedQuery = normalize(query);
        String normalizedSourceType = normalize(sourceType);
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.ASC, "title"))
        );

        Page<KnowledgeDocument> documents;
        if (!normalizedSourceType.isEmpty() && !normalizedQuery.isEmpty()) {
            documents = knowledgeDocumentRepository.findBySourceTypeIgnoreCaseAndTitleContainingIgnoreCase(
                    normalizedSourceType,
                    normalizedQuery,
                    pageRequest
            );
        } else if (!normalizedSourceType.isEmpty()) {
            documents = knowledgeDocumentRepository.findBySourceTypeIgnoreCase(normalizedSourceType, pageRequest);
        } else if (!normalizedQuery.isEmpty()) {
            documents = knowledgeDocumentRepository.findByTitleContainingIgnoreCase(normalizedQuery, pageRequest);
        } else {
            documents = knowledgeDocumentRepository.findAll(pageRequest);
        }

        List<KnowledgeDocumentSummaryResponse> items = documents.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new KnowledgeDocumentPageResponse(
                items,
                documents.getNumber(),
                documents.getSize(),
                documents.getTotalElements(),
                documents.getTotalPages(),
                documents.isFirst(),
                documents.isLast()
        );
    }

    private KnowledgeDocumentSummaryResponse toResponse(KnowledgeDocument document) {
        return new KnowledgeDocumentSummaryResponse(
                document.getId(),
                document.getTitle(),
                document.getSourceType(),
                document.getContent(),
                document.getTags() == null ? List.of() : document.getTags(),
                document.getMetadata() == null ? Map.of() : document.getMetadata(),
                document.getCreatedAt()
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}

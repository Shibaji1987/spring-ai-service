package com.agentic.ai.spring_ai_service.audit.dto.response;

import java.util.List;

public record KnowledgeDocumentPageResponse(
        List<KnowledgeDocumentSummaryResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}

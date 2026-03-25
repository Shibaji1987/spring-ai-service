package com.agentic.ai.spring_ai_service.audit.dto.response;

public record KnowledgeSearchResponse(
        String documentId,
        String documentTitle,
        Integer chunkIndex,
        String text
) {
}
package com.agentic.ai.spring_ai_service.audit.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record KnowledgeDocumentSummaryResponse(
        String id,
        String title,
        String sourceType,
        String content,
        List<String> tags,
        Map<String, Object> metadata,
        LocalDateTime createdAt
) {
}

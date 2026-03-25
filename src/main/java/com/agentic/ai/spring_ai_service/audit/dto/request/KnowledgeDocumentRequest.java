package com.agentic.ai.spring_ai_service.audit.dto.request;

import java.util.List;
import java.util.Map;

public record KnowledgeDocumentRequest(
        String title,
        String sourceType,
        String content,
        List<String> tags,
        Map<String, Object> metadata
) {
}
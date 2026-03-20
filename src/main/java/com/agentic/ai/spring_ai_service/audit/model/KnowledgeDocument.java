package com.agentic.ai.spring_ai_service.audit.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "knowledge_documents")
public class KnowledgeDocument {

    @Id
    private String id;

    private String title;
    private String sourceType;
    private String content;
    private List<String> tags;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;

    public KnowledgeDocument() {
        this.createdAt = LocalDateTime.now();
    }

    public KnowledgeDocument(String title,
                             String sourceType,
                             String content,
                             List<String> tags,
                             Map<String, Object> metadata) {
        this.title = title;
        this.sourceType = sourceType;
        this.content = content;
        this.tags = tags;
        this.metadata = metadata;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getContent() {
        return content;
    }

    public List<String> getTags() {
        return tags;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
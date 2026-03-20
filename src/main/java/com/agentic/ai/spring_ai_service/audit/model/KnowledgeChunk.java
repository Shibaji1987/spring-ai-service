package com.agentic.ai.spring_ai_service.audit.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "knowledge_chunks")
public class KnowledgeChunk {

    @Id
    private String id;

    private String documentId;
    private String documentTitle;
    private Integer chunkIndex;
    private String text;
    private List<Double> embedding;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;

    public KnowledgeChunk() {
        this.createdAt = LocalDateTime.now();
    }

    public KnowledgeChunk(String documentId,
                          String documentTitle,
                          Integer chunkIndex,
                          String text,
                          List<Double> embedding,
                          Map<String, Object> metadata) {
        this.documentId = documentId;
        this.documentTitle = documentTitle;
        this.chunkIndex = chunkIndex;
        this.text = text;
        this.embedding = embedding;
        this.metadata = metadata;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getDocumentTitle() {
        return documentTitle;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public String getText() {
        return text;
    }

    public List<Double> getEmbedding() {
        return embedding;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public void setDocumentTitle(String documentTitle) {
        this.documentTitle = documentTitle;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setEmbedding(List<Double> embedding) {
        this.embedding = embedding;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
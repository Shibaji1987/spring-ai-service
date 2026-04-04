package com.agentic.ai.spring_ai_service.audit.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Document(collection = "knowledge_chunks")
public class KnowledgeChunk {

    @Id
    private String id;

    @Setter
    private String documentId;
    @Setter
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

    @Override
    public String toString() {
        return "KnowledgeChunk{" +
                "id='" + id + '\'' +
                ", documentId='" + documentId + '\'' +
                ", documentTitle='" + documentTitle + '\'' +
                ", chunkIndex=" + chunkIndex +
                ", text='" + text + '\'' +
                ", embedding=" + embedding +
                ", metadata=" + metadata +
                ", createdAt=" + createdAt +
                '}';
    }
}
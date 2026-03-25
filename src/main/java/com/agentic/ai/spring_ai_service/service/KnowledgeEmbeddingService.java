package com.agentic.ai.spring_ai_service.service;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class KnowledgeEmbeddingService {

    private final EmbeddingModel embeddingModel;

    public KnowledgeEmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public List<Double> embed(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text cannot be null or blank for embedding.");
        }

        float[] embeddingArray = embeddingModel.embed(text);

        List<Double> embeddingList = new ArrayList<>(embeddingArray.length);

        for (float value : embeddingArray) {
            embeddingList.add((double) value);
        }

        return embeddingList;
    }
}
package com.agentic.ai.spring_ai_service.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class KnowledgeChunkingService {

    private static final int DEFAULT_CHUNK_SIZE = 800;
    private static final int DEFAULT_OVERLAP = 150;

    public List<String> chunk(String content) {
        return chunk(content, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
    }

    public List<String> chunk(String content, int chunkSize, int overlap) {
        if (content == null || content.isBlank()) {
            return Collections.emptyList();
        }

        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be greater than 0");
        }

        if (overlap < 0) {
            throw new IllegalArgumentException("overlap cannot be negative");
        }

        if (overlap >= chunkSize) {
            throw new IllegalArgumentException("overlap must be smaller than chunkSize");
        }

        String normalized = normalize(content);
        List<String> chunks = new ArrayList<>();

        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + chunkSize, normalized.length());

            if (end < normalized.length()) {
                int lastWhitespace = findLastWhitespace(normalized, start, end);
                if (lastWhitespace > start + (chunkSize / 2)) {
                    end = lastWhitespace;
                }
            }

            String chunk = normalized.substring(start, end).trim();
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }

            if (end >= normalized.length()) {
                break;
            }

            start = Math.max(end - overlap, start + 1);
        }

        return chunks;
    }

    private String normalize(String content) {
        return content
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\t ]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private int findLastWhitespace(String text, int start, int end) {
        for (int i = end - 1; i > start; i--) {
            if (Character.isWhitespace(text.charAt(i))) {
                return i;
            }
        }
        return -1;
    }
}
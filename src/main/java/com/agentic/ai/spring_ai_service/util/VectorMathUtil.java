package com.agentic.ai.spring_ai_service.util;

import java.util.List;

public final class VectorMathUtil {

    private VectorMathUtil() {
    }

    public static double cosineSimilarity(List<Double> a, List<Double> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }

        if (a.size() != b.size()) {
            throw new IllegalArgumentException("Vectors must have the same size.");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.size(); i++) {
            double valueA = a.get(i);
            double valueB = b.get(i);

            dotProduct += valueA * valueB;
            normA += valueA * valueA;
            normB += valueB * valueB;
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
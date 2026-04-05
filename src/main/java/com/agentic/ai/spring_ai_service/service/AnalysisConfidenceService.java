package com.agentic.ai.spring_ai_service.service;

import org.springframework.stereotype.Service;

@Service
public class AnalysisConfidenceService {

    public double computeConfidenceScore(
            int evidenceCount,
            int successfulToolCount,
            boolean fallbackUsed,
            boolean grounded,
            int riskScore
    ) {
        double score = 0.35;

        score += Math.min(evidenceCount, 3) * 0.15;
        score += Math.min(successfulToolCount, 3) * 0.10;
        score += grounded ? 0.10 : 0.0;
        score += riskScore >= 7 ? 0.05 : 0.0;
        score -= fallbackUsed ? 0.30 : 0.0;

        return Math.max(0.05, Math.min(0.99, score));
    }

    public String toConfidenceLabel(double score) {
        if (score >= 0.80) return "HIGH";
        if (score >= 0.55) return "MEDIUM";
        return "LOW";
    }
}
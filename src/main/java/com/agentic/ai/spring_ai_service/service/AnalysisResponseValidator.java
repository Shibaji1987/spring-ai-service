package com.agentic.ai.spring_ai_service.service;

import com.agentic.ai.spring_ai_service.audit.dto.agent.AgentFinalizePayload;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AnalysisResponseValidator {

    public AgentFinalizePayload normalize(AgentFinalizePayload payload) {
        if (payload == null) {
            return AgentFinalizePayload.builder()
                    .riskScore(0)
                    .category("UNKNOWN")
                    .summary("No analysis generated.")
                    .reasons(List.of())
                    .tags(List.of())
                    .recommendedAction("Manual review recommended.")
                    .fallbackUsed(true)
                    .build();
        }

        payload.setRiskScore(clamp(payload.getRiskScore(), 0, 10));
        payload.setCategory(defaultString(payload.getCategory(), "UNKNOWN"));
        payload.setSummary(defaultString(payload.getSummary(), "No analysis generated."));
        payload.setReasons(safeList(payload.getReasons()));
        payload.setTags(safeList(payload.getTags()));
        payload.setRecommendedAction(defaultString(payload.getRecommendedAction(), "Manual review recommended."));
        payload.setFallbackUsed(payload.getFallbackUsed() != null ? payload.getFallbackUsed() : Boolean.FALSE);

        return payload;
    }

    private int clamp(Integer value, int min, int max) {
        int safe = value == null ? min : value;
        return Math.max(min, Math.min(max, safe));
    }

    private String defaultString(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private List<String> safeList(List<String> input) {
        return input == null ? new ArrayList<>() : input;
    }
}
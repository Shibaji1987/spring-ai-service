package com.agentic.ai.spring_ai_service.service;

import com.agentic.ai.spring_ai_service.audit.dto.AuditAiResponse;

import com.agentic.ai.spring_ai_service.audit.dto.AuditAnalysisResultDto;
import com.agentic.ai.spring_ai_service.audit.mapper.AuditAnalysisMapper;
import com.agentic.ai.spring_ai_service.audit.model.AuditAiAnalysis;
import com.agentic.ai.spring_ai_service.audit.model.AuditEvent;

import com.agentic.ai.spring_ai_service.audit.dto.AuditAnalyzeRequest;
import com.agentic.ai.spring_ai_service.repository.AuditAiAnalysisRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class AuditAnalysisService {

    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            "AUTHENTICATION_RISK",
            "AUTHORIZATION_VIOLATION",
            "DATA_ACCESS_ANOMALY",
            "TRANSACTION_RISK",
            "CONFIGURATION_CHANGE_RISK",
            "COMPLIANCE_ALERT",
            "INSIDER_THREAT",
            "LOW_RISK_ACTIVITY"
    );

    private final AuditEventService auditEventService;
    private final AuditAiService auditAiService;
    private final AuditAiAnalysisRepository auditAiAnalysisRepository;
    private final ObjectMapper objectMapper;

    public AuditAnalysisService(AuditEventService auditEventService,
                                AuditAiService auditAiService,
                                AuditAiAnalysisRepository auditAiAnalysisRepository,
                                ObjectMapper objectMapper) {
        this.auditEventService = auditEventService;
        this.auditAiService = auditAiService;
        this.auditAiAnalysisRepository = auditAiAnalysisRepository;
        this.objectMapper = objectMapper;
    }

    public AuditAnalysisResultDto analyze(AuditAnalyzeRequest request) {
        AuditEvent savedEvent = auditEventService.save(request);

        String aiJson = auditAiService.analyze(savedEvent);

        AuditAiResponse parsedResponse = parseAndNormalize(aiJson);

        AuditAiAnalysis analysis = new AuditAiAnalysis();
        analysis.setAuditEventId(savedEvent.getId());
        analysis.setRiskScore(parsedResponse.getRiskScore());
        analysis.setCategory(parsedResponse.getCategory());
        analysis.setSummary(parsedResponse.getSummary());
        analysis.setReasons(parsedResponse.getReasons());
        analysis.setTags(parsedResponse.getTags());
        analysis.setRecommendedAction(parsedResponse.getRecommendedAction());

        AuditAiAnalysis savedAnalysis = auditAiAnalysisRepository.save(analysis);

        return AuditAnalysisMapper.toDto(savedEvent, savedAnalysis);
    }

    public List<AuditAiAnalysis> getAllAnalysis() {
        return auditAiAnalysisRepository.findAll();
    }

    public AuditAiAnalysis getAnalysisByEventId(String auditEventId) {
        return auditAiAnalysisRepository.findByAuditEventId(auditEventId)
                .orElseThrow(() -> new RuntimeException("AI analysis not found for eventId: " + auditEventId));
    }

    public AuditAnalysisResultDto getFullAnalysis(String eventId) {
        AuditEvent event = auditEventService.getEventById(eventId);
        AuditAiAnalysis analysis = getAnalysisByEventId(eventId);
        return AuditAnalysisMapper.toDto(event, analysis);
    }

    private AuditAiResponse parseAndNormalize(String aiJson) {
        try {
            AuditAiResponse response = objectMapper.readValue(aiJson, AuditAiResponse.class);
            return normalize(response);
        } catch (Exception e) {
            AuditAiResponse fallback = new AuditAiResponse();
            fallback.setRiskScore(5);
            fallback.setCategory("COMPLIANCE_ALERT");
            fallback.setSummary("AI response could not be parsed cleanly, so a fallback analysis was stored.");
            fallback.setReasons(List.of("parse_failure", "fallback_applied"));
            fallback.setTags(List.of("ai_parse_issue", "fallback"));
            fallback.setRecommendedAction("Review the event manually and inspect the raw AI response.");
            return fallback;
        }
    }

    private AuditAiResponse normalize(AuditAiResponse response) {
        if (response == null) {
            response = new AuditAiResponse();
        }

        if (response.getRiskScore() == null) {
            response.setRiskScore(5);
        }

        if (response.getRiskScore() < 1) {
            response.setRiskScore(1);
        }

        if (response.getRiskScore() > 10) {
            response.setRiskScore(10);
        }

        if (response.getCategory() == null || !ALLOWED_CATEGORIES.contains(response.getCategory())) {
            response.setCategory("COMPLIANCE_ALERT");
        }

        if (response.getSummary() == null || response.getSummary().isBlank()) {
            response.setSummary("AI generated audit analysis.");
        } else {
            response.setSummary(response.getSummary().trim());
        }

        if (response.getReasons() == null) {
            response.setReasons(List.of());
        }

        if (response.getTags() == null) {
            response.setTags(List.of());
        }

        if (response.getRecommendedAction() == null || response.getRecommendedAction().isBlank()) {
            response.setRecommendedAction("Review the event and validate whether follow-up is required.");
        } else {
            response.setRecommendedAction(response.getRecommendedAction().trim());
        }

        return response;
    }
}
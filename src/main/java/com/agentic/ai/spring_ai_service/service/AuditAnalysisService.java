package com.agentic.ai.spring_ai_service.service;

import com.agentic.ai.spring_ai_service.audit.dto.AuditAiResponse;
import com.agentic.ai.spring_ai_service.audit.dto.AuditAnalyzeResult;
import com.agentic.ai.spring_ai_service.audit.dto.AuditEventRequest;
import com.agentic.ai.spring_ai_service.audit.model.AuditAiAnalysis;
import com.agentic.ai.spring_ai_service.audit.model.AuditEvent;
import com.agentic.ai.spring_ai_service.repository.AuditAiAnalysisRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class AuditAnalysisService {

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

    public AuditAnalyzeResult analyzeAndSave(AuditEventRequest request) throws Exception {
        AuditEvent savedEvent = auditEventService.saveAuditEvent(request);

        String aiJson = auditAiService.analyzeEvent(savedEvent);
        AuditAiResponse aiResponse = objectMapper.readValue(aiJson, AuditAiResponse.class);

        AuditAiAnalysis analysis = new AuditAiAnalysis();
        analysis.setAuditEventId(savedEvent.getId());
        analysis.setRiskScore(aiResponse.getRiskScore());
        analysis.setCategory(aiResponse.getCategory());
        analysis.setSummary(aiResponse.getSummary());
        analysis.setReasons(aiResponse.getReasons());
        analysis.setTags(aiResponse.getTags());
        analysis.setRecommendedAction(aiResponse.getRecommendedAction());

        AuditAiAnalysis savedAnalysis = auditAiAnalysisRepository.save(analysis);

        AuditAnalyzeResult result = new AuditAnalyzeResult();
        result.setEventId(savedEvent.getId());
        result.setAnalysisId(savedAnalysis.getId());
        result.setRiskScore(savedAnalysis.getRiskScore());
        result.setCategory(savedAnalysis.getCategory());
        result.setSummary(savedAnalysis.getSummary());
        result.setRecommendedAction(savedAnalysis.getRecommendedAction());

        return result;
    }

    public AuditAiAnalysis getAnalysisByEventId(String auditEventId) {
        return auditAiAnalysisRepository.findByAuditEventId(auditEventId)
                .orElseThrow(() -> new RuntimeException("Analysis not found for event id: " + auditEventId));
    }
}
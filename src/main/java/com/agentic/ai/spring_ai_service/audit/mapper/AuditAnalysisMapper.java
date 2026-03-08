package com.agentic.ai.spring_ai_service.audit.mapper;

import com.agentic.ai.spring_ai_service.audit.dto.response.AuditAnalysisResultDto;
import com.agentic.ai.spring_ai_service.audit.model.AuditAiAnalysis;
import com.agentic.ai.spring_ai_service.audit.model.AuditEvent;

public class AuditAnalysisMapper {

    private AuditAnalysisMapper() {
    }

    public static AuditAnalysisResultDto toDto(AuditEvent event, AuditAiAnalysis analysis) {
        AuditAnalysisResultDto dto = new AuditAnalysisResultDto();

        dto.setAuditEventId(event.getId());
        dto.setEventType(event.getEventType());
        dto.setActor(event.getActor());
        dto.setAction(event.getAction());
        dto.setTarget(event.getTarget());
        dto.setStatus(event.getStatus());
        dto.setEventTime(event.getEventTime());
        dto.setMetadata(event.getMetadata());

        dto.setRiskScore(analysis.getRiskScore());
        dto.setCategory(analysis.getCategory());
        dto.setSummary(analysis.getSummary());
        dto.setReasons(analysis.getReasons());
        dto.setTags(analysis.getTags());
        dto.setRecommendedAction(analysis.getRecommendedAction());

        return dto;
    }
}
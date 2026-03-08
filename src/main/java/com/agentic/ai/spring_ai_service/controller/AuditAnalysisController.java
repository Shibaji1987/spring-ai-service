package com.agentic.ai.spring_ai_service.controller;

import com.agentic.ai.spring_ai_service.audit.dto.AuditAnalyzeResult;
import com.agentic.ai.spring_ai_service.audit.dto.AuditEventRequest;
import com.agentic.ai.spring_ai_service.service.AuditAnalysisService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/audit")
public class AuditAnalysisController {

    private final AuditAnalysisService auditAnalysisService;

    public AuditAnalysisController(AuditAnalysisService auditAnalysisService) {
        this.auditAnalysisService = auditAnalysisService;
    }

    @PostMapping("/analyze")
    public AuditAnalyzeResult analyze(@RequestBody AuditEventRequest request) throws Exception {
        return auditAnalysisService.analyzeAndSave(request);
    }

    @GetMapping("/analysis/event/{eventId}")
    public com.agentic.ai.spring_ai_service.audit.model.AuditAiAnalysis getAnalysisByEventId(@PathVariable String eventId) {
        return auditAnalysisService.getAnalysisByEventId(eventId);
    }
}
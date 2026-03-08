package com.agentic.ai.spring_ai_service.audit.controller;


import com.agentic.ai.spring_ai_service.audit.dto.response.AuditAnalysisResultDto;
import com.agentic.ai.spring_ai_service.audit.model.AuditAiAnalysis;
import com.agentic.ai.spring_ai_service.audit.dto.request.AuditAnalyzeRequest;
import com.agentic.ai.spring_ai_service.service.AuditAnalysisService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/audit")
public class AuditAnalysisController {

    private final AuditAnalysisService auditAnalysisService;

    public AuditAnalysisController(AuditAnalysisService auditAnalysisService) {
        this.auditAnalysisService = auditAnalysisService;
    }

    @PostMapping("/analyze")
    public AuditAnalysisResultDto analyze(@RequestBody AuditAnalyzeRequest request) {
        return auditAnalysisService.analyze(request);
    }

    @GetMapping("/analysis")
    public List<AuditAiAnalysis> getAllAnalysis() {
        return auditAnalysisService.getAllAnalysis();
    }

    @GetMapping("/analysis/{eventId}")
    public AuditAiAnalysis getAnalysisByEventId(@PathVariable String eventId) {
        return auditAnalysisService.getAnalysisByEventId(eventId);
    }

    @GetMapping("/full/{eventId}")
    public AuditAnalysisResultDto getFullAnalysis(@PathVariable String eventId) {
        return auditAnalysisService.getFullAnalysis(eventId);
    }
}
package com.agentic.ai.spring_ai_service.audit.controller;

import com.agentic.ai.spring_ai_service.audit.dto.response.AuditAnalyzeResponse;
import com.agentic.ai.spring_ai_service.service.AuditAgentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/audit")
public class AuditAgentController {

    private final AuditAgentService auditAgentService;

    public AuditAgentController(AuditAgentService auditAgentService) {
        this.auditAgentService = auditAgentService;
    }

    @PostMapping("/analyze-with-tools/{eventId}")
    public AuditAnalyzeResponse analyzeWithTools(@PathVariable String eventId) {
        return auditAgentService.analyzeEventWithTools(eventId);
    }
}
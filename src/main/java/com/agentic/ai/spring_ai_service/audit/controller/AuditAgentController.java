package com.agentic.ai.spring_ai_service.audit.controller;

import com.agentic.ai.spring_ai_service.audit.dto.response.AuditAnalyzeResponse;
import com.agentic.ai.spring_ai_service.service.AuditAgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/audit")
public class AuditAgentController {

    private static final Logger log = LoggerFactory.getLogger(AuditAgentController.class);
    private final AuditAgentService auditAgentService;

    public AuditAgentController(AuditAgentService auditAgentService) {
        this.auditAgentService = auditAgentService;
    }

    @PostMapping("/analyze-with-tools/{eventId}")
    public AuditAnalyzeResponse analyzeWithTools(@PathVariable String eventId) {
        log.info("Invoking Agent");
        return auditAgentService.analyzeEventWithTools(eventId);
    }
}
package com.agentic.ai.spring_ai_service.service;

import com.agentic.ai.spring_ai_service.audit.model.AuditEvent;
import com.agentic.ai.spring_ai_service.audit.model.MatchedPolicyEvidence;

import java.util.List;

public record AuditAnalysisContext(
        AuditEvent auditEvent,
        List<MatchedPolicyEvidence> policyEvidence
) {
}

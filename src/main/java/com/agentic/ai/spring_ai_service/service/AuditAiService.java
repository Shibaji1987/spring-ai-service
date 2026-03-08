package com.agentic.ai.spring_ai_service.service;


import com.agentic.ai.spring_ai_service.audit.model.AuditEvent;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AuditAiService {

    private final ChatClient chatClient;

    public AuditAiService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String analyzeEvent(AuditEvent event) {
        String prompt = """
            You are a banking audit risk analyzer.

            Analyze the audit event and return only valid JSON.

            Allowed category values:
            NORMAL_ACTIVITY
            SUSPICIOUS_LOGIN
            FAILED_LOGIN_PATTERN
            PRIVILEGE_CHANGE
            DATA_ACCESS_ANOMALY
            POTENTIAL_ACCOUNT_TAKEOVER

            riskScore must be from 1 to 10.

            If the event only looks unusual but not confirmed malicious,
            prefer SUSPICIOUS_LOGIN instead of POTENTIAL_ACCOUNT_TAKEOVER.

            Required JSON format:
            {
              "riskScore": 1,
              "category": "string",
              "summary": "string",
              "reasons": ["string"],
              "tags": ["string"],
              "recommendedAction": "string"
            }

            Audit Event:
            """ + event;

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}
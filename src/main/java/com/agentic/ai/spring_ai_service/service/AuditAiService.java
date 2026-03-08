package com.agentic.ai.spring_ai_service.service;

import com.agentic.ai.spring_ai_service.audit.model.AuditEvent;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AuditAiService {

    private final ChatClient chatClient;

    public AuditAiService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String analyze(AuditEvent event) {
        String prompt = """
                You are a banking audit risk analysis engine.

                Analyze the audit event and return ONLY valid JSON.
                Do not return markdown, explanation, headings, or code fences.

                Allowed category values:
                - AUTHENTICATION_RISK
                - AUTHORIZATION_VIOLATION
                - DATA_ACCESS_ANOMALY
                - TRANSACTION_RISK
                - CONFIGURATION_CHANGE_RISK
                - COMPLIANCE_ALERT
                - INSIDER_THREAT
                - LOW_RISK_ACTIVITY

                Category guidance:
                - suspicious login, impossible travel, new device, odd hour login -> AUTHENTICATION_RISK
                - forbidden action, access denied, role misuse, privilege abuse -> AUTHORIZATION_VIOLATION
                - bulk export, excessive reads, sensitive record access, unusual data retrieval -> DATA_ACCESS_ANOMALY
                - suspicious payment, abnormal transfer, unusual monetary activity -> TRANSACTION_RISK
                - configuration update, role change, policy change, audit setting change -> CONFIGURATION_CHANGE_RISK
                - KYC/AML/privacy/regulatory risk -> COMPLIANCE_ALERT
                - employee misuse, repeated suspicious internal activity -> INSIDER_THREAT
                - clearly normal benign activity -> LOW_RISK_ACTIVITY

                Rules:
                1. riskScore must be an integer between 1 and 10
                2. category must be exactly one allowed value
                3. summary must be one concise sentence
                4. reasons must contain 2 to 5 short strings
                5. tags must contain 3 to 6 lowercase strings using underscores
                6. recommendedAction must be one practical sentence
                7. output must be strictly valid JSON

                JSON format:
                {
                  "riskScore": 0,
                  "category": "",
                  "summary": "",
                  "reasons": [],
                  "tags": [],
                  "recommendedAction": ""
                }

                Audit event:
                eventType: %s
                actor: %s
                action: %s
                target: %s
                status: %s
                eventTime: %s
                metadata: %s
                """.formatted(
                event.getEventType(),
                event.getActor(),
                event.getAction(),
                event.getTarget(),
                event.getStatus(),
                event.getEventTime(),
                event.getMetadata()
        );

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}
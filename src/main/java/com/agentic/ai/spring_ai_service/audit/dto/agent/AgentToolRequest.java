package com.agentic.ai.spring_ai_service.audit.dto.agent;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentToolRequest {
    private String toolName;
    private Map<String, Object> toolArgs;
}
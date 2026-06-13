package com.agentic.ai.spring_ai_service.security;

import java.time.Instant;
import java.util.List;

public record LoginResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        String username,
        List<String> roles
) {
}

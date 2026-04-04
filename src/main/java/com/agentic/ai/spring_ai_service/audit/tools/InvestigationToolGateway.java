package com.agentic.ai.spring_ai_service.audit.tools;

import com.agentic.ai.spring_ai_service.audit.model.AuditEventToolResult;
import com.agentic.ai.spring_ai_service.audit.model.ToolExecutionRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InvestigationToolGateway {

    private final AuditInvestigationTools auditInvestigationTools;

    public ToolExecutionOutcome executeWhitelisted(String toolName, Map<String, Object> arguments) {
        Instant startedAt = Instant.now();

        try {
            return switch (toolName) {
                case "findRecentEventsByUser" -> executeFindRecentEventsByUser(arguments, startedAt);
                default -> buildFailure(toolName, arguments, startedAt, "Tool not whitelisted: " + toolName);
            };
        } catch (Exception ex) {
            return buildFailure(toolName, arguments, startedAt, ex.getMessage());
        }
    }

    private ToolExecutionOutcome executeFindRecentEventsByUser(Map<String, Object> arguments, Instant startedAt) {
        String userId = getString(arguments, "userId");
        if (userId == null || userId.isBlank()) {
            userId = getString(arguments, "actor");
        }

        Integer hours = getInteger(arguments, "hours");
        if (hours == null || hours <= 0) {
            hours = 24;
        }

        List<AuditEventToolResult> results = auditInvestigationTools.findRecentEventsByUser(userId, hours);

        Instant completedAt = Instant.now();

        ToolExecutionRecord record = ToolExecutionRecord.builder()
                .toolName("findRecentEventsByUser")
                .status("EXECUTED")
                .startedAt(startedAt)
                .completedAt(completedAt)
                .input(arguments == null ? Collections.emptyMap() : arguments)
                .output(Map.of(
                        "resultCount", results.size(),
                        "results", results
                ))
                .failureReason(null)
                .durationMs(completedAt.toEpochMilli() - startedAt.toEpochMilli())
                .build();

        String summary = "findRecentEventsByUser returned " + results.size() + " event(s) for actor=" + userId;

        return new ToolExecutionOutcome(record, summary);
    }

    private ToolExecutionOutcome buildFailure(String toolName,
                                              Map<String, Object> arguments,
                                              Instant startedAt,
                                              String reason) {
        Instant completedAt = Instant.now();

        ToolExecutionRecord record = ToolExecutionRecord.builder()
                .toolName(toolName)
                .status("FAILED")
                .startedAt(startedAt)
                .completedAt(completedAt)
                .input(arguments == null ? Collections.emptyMap() : arguments)
                .output(Collections.emptyMap())
                .failureReason(reason)
                .durationMs(completedAt.toEpochMilli() - startedAt.toEpochMilli())
                .build();

        return new ToolExecutionOutcome(record, null);
    }

    private String getString(Map<String, Object> arguments, String key) {
        if (arguments == null || !arguments.containsKey(key) || arguments.get(key) == null) {
            return null;
        }
        return String.valueOf(arguments.get(key));
    }

    private Integer getInteger(Map<String, Object> arguments, String key) {
        if (arguments == null || !arguments.containsKey(key) || arguments.get(key) == null) {
            return null;
        }

        Object value = arguments.get(key);

        if (value instanceof Integer integer) {
            return integer;
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
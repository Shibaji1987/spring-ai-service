package com.agentic.ai.spring_ai_service.audit.tools;

import com.agentic.ai.spring_ai_service.audit.model.ToolExecutionRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvestigationToolGateway {

    private static final Set<String> ALLOWED_TOOLS = Set.of(
            "getUserActivitySummary",
            "getFailedLoginCount",
            "getRecentEvents"
    );

    private final AuditTools auditTools;

    public ToolExecutionRecord executeWhitelisted(String toolName, Map<String, Object> input) {
        if (toolName == null || toolName.isBlank()) {
            return ToolExecutionRecord.builder()
                    .toolName("UNKNOWN")
                    .success(false)
                    .durationMs(0L)
                    .inputSummary(input != null ? input.toString() : "{}")
                    .outputSummary(null)
                    .errorMessage("Tool name is missing.")
                    .executedAt(LocalDateTime.now())
                    .build();
        }

        if (!ALLOWED_TOOLS.contains(toolName)) {
            return ToolExecutionRecord.builder()
                    .toolName(toolName)
                    .success(false)
                    .durationMs(0L)
                    .inputSummary(input != null ? input.toString() : "{}")
                    .outputSummary(null)
                    .errorMessage("Tool is not whitelisted: " + toolName)
                    .executedAt(LocalDateTime.now())
                    .build();
        }

        return switch (toolName) {
            case "getUserActivitySummary" -> {
                String actor = getString(input, "actor");
                yield execute(
                        toolName,
                        input,
                        () -> formatUserActivitySummary(actor, auditTools.getUserActivitySummary(actor))
                );
            }
            case "getFailedLoginCount" -> {
                String actor = getString(input, "actor");
                yield execute(
                        toolName,
                        input,
                        () -> formatFailedLoginCount(actor, auditTools.getFailedLoginCount(actor))
                );
            }
            case "getRecentEvents" -> {
                String actor = getString(input, "actor");
                int limit = getInt(input, "limit", 5);
                yield execute(
                        toolName,
                        input,
                        () -> formatRecentEvents(actor, limit, auditTools.getRecentEvents(actor, limit))
                );
            }
            default -> ToolExecutionRecord.builder()
                    .toolName(toolName)
                    .success(false)
                    .durationMs(0L)
                    .inputSummary(input != null ? input.toString() : "{}")
                    .outputSummary(null)
                    .errorMessage("Unsupported tool: " + toolName)
                    .executedAt(LocalDateTime.now())
                    .build();
        };
    }

    public ToolExecutionRecord execute(String toolName, String inputSummary, Supplier<Object> supplier) {
        long start = System.currentTimeMillis();

        try {
            Object result = supplier.get();
            long duration = System.currentTimeMillis() - start;

            ToolExecutionRecord record = ToolExecutionRecord.builder()
                    .toolName(toolName)
                    .success(true)
                    .durationMs(duration)
                    .inputSummary(inputSummary)
                    .outputSummary(result != null ? String.valueOf(result) : "null")
                    .errorMessage(null)
                    .executedAt(LocalDateTime.now())
                    .build();

            log.info("Tool executed successfully. tool={} durationMs={}", toolName, duration);
            return record;

        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - start;

            ToolExecutionRecord record = ToolExecutionRecord.builder()
                    .toolName(toolName)
                    .success(false)
                    .durationMs(duration)
                    .inputSummary(inputSummary)
                    .outputSummary(null)
                    .errorMessage(ex.getMessage())
                    .executedAt(LocalDateTime.now())
                    .build();

            log.warn("Tool execution failed. tool={} durationMs={} error={}", toolName, duration, ex.getMessage());
            return record;
        }
    }

    public ToolExecutionRecord execute(String toolName, Map<String, Object> input, Supplier<Object> supplier) {
        return execute(toolName, input != null ? input.toString() : "{}", supplier);
    }

    private String formatUserActivitySummary(String actor, Object raw) {
        return "User activity summary for " + actor + ": " + safe(raw);
    }

    private String formatFailedLoginCount(String actor, Object raw) {
        return "Failed login count for " + actor + ": " + safe(raw);
    }

    private String formatRecentEvents(String actor, int limit, Object raw) {
        if (raw instanceof List<?> list) {
            String summary = list.stream()
                    .limit(3)
                    .map(String::valueOf)
                    .collect(Collectors.joining(" | "));
            return "Recent events for " + actor + " limit=" + limit + ": count=" + list.size() + (summary.isBlank() ? "" : " -> " + summary);
        }
        return "Recent events for " + actor + " limit=" + limit + ": " + safe(raw);
    }

    private String getString(Map<String, Object> input, String key) {
        if (input == null || !input.containsKey(key) || input.get(key) == null) {
            return "";
        }
        return String.valueOf(input.get(key));
    }

    private int getInt(Map<String, Object> input, String key, int defaultValue) {
        if (input == null || !input.containsKey(key) || input.get(key) == null) {
            return defaultValue;
        }

        Object value = input.get(key);

        if (value instanceof Number number) {
            return number.intValue();
        }

        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private String safe(Object value) {
        return value == null ? "null" : String.valueOf(value);
    }
}
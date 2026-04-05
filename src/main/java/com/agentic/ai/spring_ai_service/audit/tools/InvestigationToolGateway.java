package com.agentic.ai.spring_ai_service.audit.tools;

import com.agentic.ai.spring_ai_service.audit.model.ToolExecutionRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

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
                String actor = getString(input);
                yield execute(
                        toolName,
                        input,
                        () -> auditTools.getUserActivitySummary(actor)
                );
            }
            case "getFailedLoginCount" -> {
                String actor = getString(input);
                yield execute(
                        toolName,
                        input,
                        () -> auditTools.getFailedLoginCount(actor)
                );
            }
            case "getRecentEvents" -> {
                String actor = getString(input);
                int limit = getInt(input);
                yield execute(
                        toolName,
                        input,
                        () -> auditTools.getRecentEvents(actor, limit)
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

    private String getString(Map<String, Object> input) {
        if (input == null || !input.containsKey("actor") || input.get("actor") == null) {
            return "";
        }
        return String.valueOf(input.get("actor"));
    }

    private int getInt(Map<String, Object> input) {
        if (input == null || !input.containsKey("limit") || input.get("limit") == null) {
            return 5;
        }

        Object value = input.get("limit");

        if (value instanceof Number number) {
            return number.intValue();
        }

        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 5;
        }
    }
}
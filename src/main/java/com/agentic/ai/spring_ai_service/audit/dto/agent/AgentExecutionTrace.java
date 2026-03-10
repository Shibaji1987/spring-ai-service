package com.agentic.ai.spring_ai_service.audit.dto.agent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AgentExecutionTrace {

    private final String traceId = UUID.randomUUID().toString();
    private final Instant startedAt = Instant.now();
    private Instant finishedAt;

    private String eventId;
    private String actor;
    private int maxToolCalls;
    private int toolCallsAttempted;
    private int toolCallsSucceeded;
    private int toolCallsFailed;
    private String status;

    private final List<String> toolsUsed = new ArrayList<>();
    private final List<String> notes = new ArrayList<>();

    public String getTraceId() {
        return traceId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void finish() {
        this.finishedAt = Instant.now();
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public int getMaxToolCalls() {
        return maxToolCalls;
    }

    public void setMaxToolCalls(int maxToolCalls) {
        this.maxToolCalls = maxToolCalls;
    }

    public int getToolCallsAttempted() {
        return toolCallsAttempted;
    }

    public void incrementAttempted() {
        this.toolCallsAttempted++;
    }

    public int getToolCallsSucceeded() {
        return toolCallsSucceeded;
    }

    public void incrementSucceeded() {
        this.toolCallsSucceeded++;
    }

    public int getToolCallsFailed() {
        return toolCallsFailed;
    }

    public void incrementFailed() {
        this.toolCallsFailed++;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getToolsUsed() {
        return toolsUsed;
    }

    public void addTool(String toolName) {
        this.toolsUsed.add(toolName);
    }

    public List<String> getNotes() {
        return notes;
    }

    public void addNote(String note) {
        this.notes.add(note);
    }
}
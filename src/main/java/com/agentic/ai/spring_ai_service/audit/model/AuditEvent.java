package com.agentic.ai.spring_ai_service.audit.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "audit_events")
public class AuditEvent {

    @Id
    private String id;

    private String eventType;
    private String actor;
    private String action;
    private String target;
    private String status;
    private LocalDateTime eventTime;
    private Map<String, Object> metadata;

    public AuditEvent() {
    }

    public AuditEvent(String eventType, String actor, String action, String target,
                      String status, LocalDateTime eventTime, Map<String, Object> metadata) {
        this.eventType = eventType;
        this.actor = actor;
        this.action = action;
        this.target = target;
        this.status = status;
        this.eventTime = eventTime;
        this.metadata = metadata;
    }

    public String getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "AuditEvent{" +
                "id='" + id + '\'' +
                ", eventType='" + eventType + '\'' +
                ", actor='" + actor + '\'' +
                ", action='" + action + '\'' +
                ", target='" + target + '\'' +
                ", status='" + status + '\'' +
                ", eventTime=" + eventTime +
                ", metadata=" + metadata +
                '}';
    }
}
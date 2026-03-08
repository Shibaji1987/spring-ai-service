package com.agentic.ai.spring_ai_service.audit.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "audit_ai_analysis")
public class AuditAiAnalysis {

    @Id
    private String id;
    private String auditEventId;
    private Integer riskScore;
    private String category;
    private String summary;
    private List<String> reasons;
    private List<String> tags;
    private String recommendedAction;

    public String getId() {
        return id;
    }

    public String getAuditEventId() {
        return auditEventId;
    }

    public void setAuditEventId(String auditEventId) {
        this.auditEventId = auditEventId;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }
}
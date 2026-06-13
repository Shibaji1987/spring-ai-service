package com.agentic.ai.spring_ai_service.audit.repository;

import com.agentic.ai.spring_ai_service.audit.model.AuditAiAnalysis;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.count;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;

@Repository
public class DashboardAnalyticsRepository {

    private final MongoTemplate mongoTemplate;

    public DashboardAnalyticsRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public long countAnalyzedEvents() {
        return countDistinctEventIds();
    }

    public long countAnalyzedEventsBetween(Instant start, Instant end) {
        return countDistinctEventIds(Criteria.where("analyzedAt").gte(start).lt(end));
    }

    public long countHighRiskEvents(int threshold) {
        return countDistinctEventIds(Criteria.where("riskScore").gte(threshold));
    }

    public long countHighRiskEventsBetween(int threshold, Instant start, Instant end) {
        return countDistinctEventIds(
                Criteria.where("riskScore").gte(threshold),
                Criteria.where("analyzedAt").gte(start).lt(end)
        );
    }

    public long countPolicyMatchedEvents() {
        return countDistinctEventIds(Criteria.where("grounded").is(true));
    }

    public long countPolicyMatchedEventsBetween(Instant start, Instant end) {
        return countDistinctEventIds(
                Criteria.where("grounded").is(true),
                Criteria.where("analyzedAt").gte(start).lt(end)
        );
    }

    public long countHighRiskPolicyMatchedEventsBetween(int threshold, Instant start, Instant end) {
        return countDistinctEventIds(
                Criteria.where("riskScore").gte(threshold),
                Criteria.where("grounded").is(true),
                Criteria.where("analyzedAt").gte(start).lt(end)
        );
    }

    private long countDistinctEventIds(Criteria... filters) {
        List<Criteria> criteria = new ArrayList<>();
        criteria.add(Criteria.where("eventId").exists(true).ne(null));
        criteria.addAll(List.of(filters));

        Aggregation aggregation = newAggregation(
                match(new Criteria().andOperator(criteria.toArray(Criteria[]::new))),
                group("eventId"),
                count().as("total")
        );
        AggregationResults<Document> results = mongoTemplate.aggregate(
                aggregation,
                AuditAiAnalysis.class,
                Document.class
        );
        Document countResult = results.getUniqueMappedResult();
        Number total = countResult == null ? null : countResult.get("total", Number.class);
        return total == null ? 0 : total.longValue();
    }
}

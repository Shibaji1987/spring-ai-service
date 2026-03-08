package com.agentic.ai.spring_ai_service.audit.dto;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuditHistoryStore {

    public record StoredEvent(String event, Instant timestamp) {}

    private final Map<String, Deque<StoredEvent>> history = new ConcurrentHashMap<>();

    @PostConstruct
    public void seedData() {
        addEvent("123", "User 123 logged in from Toronto at 09:10 AM from known device");
        addEvent("123", "User 123 checked account balance from Toronto at 09:12 AM");
        addEvent("123", "User 123 updated profile from Toronto at 09:20 AM");

        addEvent("999", "User 999 granted admin access to user 555");
        addEvent("999", "User 999 exported audit report");
    }

    public void addEvent(String userId, String event) {
        history.computeIfAbsent(userId, k -> new ArrayDeque<>());
        Deque<StoredEvent> deque = history.get(userId);

        synchronized (deque) {
            deque.addFirst(new StoredEvent(event, Instant.now()));
            while (deque.size() > 50) {
                deque.removeLast();
            }
        }
    }

    public List<StoredEvent> lastEvents(String userId, int limit) {
        Deque<StoredEvent> deque = history.getOrDefault(userId, new ArrayDeque<>());

        synchronized (deque) {
            return deque.stream()
                    .limit(Math.max(1, Math.min(limit, 10)))
                    .toList();
        }
    }

    public String userProfile(String userId) {
        return switch (userId) {
            case "123" -> "userId=123, role=customer, usual_country=Canada, usual_city=Toronto, mfa_enabled=true, normal_login_hours=08:00-22:00";
            case "999" -> "userId=999, role=admin, usual_country=Canada, usual_city=Toronto, mfa_enabled=true, high_privilege=true";
            default -> "userId=" + userId + ", role=unknown, usual_country=unknown, mfa_enabled=unknown";
        };
    }
}
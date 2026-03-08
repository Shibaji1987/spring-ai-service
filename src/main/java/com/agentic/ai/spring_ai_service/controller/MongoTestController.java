package com.agentic.ai.spring_ai_service.controller;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MongoTestController {

    private final MongoTemplate mongoTemplate;

    public MongoTestController(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping("/mongo-test")
    public String mongoTest() {
        return "Connected to DB: " + mongoTemplate.getDb().getName();
    }
}
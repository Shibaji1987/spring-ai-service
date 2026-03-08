package com.agentic.ai.spring_ai_service.audit.dto.controller;

import com.agentic.ai.spring_ai_service.service.AIService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AIController {

    private final AIService aiService;

    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/ask")
    public Map<String, String> ask(@RequestParam String question) {

        String answer = aiService.ask(question);

        return Map.of(
                "question", question,
                "answer", answer
        );
    }
}
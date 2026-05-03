package com.foody.controller;

import com.foody.dto.ChatRequest;
import com.foody.service.ChatbotService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/message")
    public Map<String, Object> message(@Valid @RequestBody ChatRequest request,
                                       Authentication auth) {
        String email = auth == null ? null : auth.getName();
        return chatbotService.reply(request.getMessage(), email);
    }
}

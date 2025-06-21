package vn.ctiep.jobhunter.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import vn.ctiep.jobhunter.domain.ChatHistory;
import vn.ctiep.jobhunter.domain.ChatRequest;
import vn.ctiep.jobhunter.domain.ChatResponse;
import vn.ctiep.jobhunter.service.OpenRouterChatbotService;

import java.util.List;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {
    private final OpenRouterChatbotService chatbotService;
    
    public ChatbotController(OpenRouterChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }
    
    @PostMapping("/ask")
    public ResponseEntity<ChatResponse> askQuestion(@RequestBody ChatRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        request.setUserId(userId);
        return ResponseEntity.ok(chatbotService.processMessage(request));
    }
    
    @GetMapping("/history")
    public ResponseEntity<List<ChatHistory>> getChatHistory() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(chatbotService.getUserChatHistory(userId));
    }
} 
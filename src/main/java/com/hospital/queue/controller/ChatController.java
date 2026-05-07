package com.hospital.queue.controller;

import com.hospital.queue.dto.response.ApiResponse;
import com.hospital.queue.dto.response.ChatMessageResponse;
import com.hospital.queue.service.ChatService;
import com.hospital.queue.util.SecurityUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService          chatService;
    private final SecurityUtils        securityUtils;
    private final SimpMessagingTemplate messagingTemplate;

    // ─── Thymeleaf view ───────────────────────────────────────────────────────

    @GetMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    public String chatRoom(@RequestParam(defaultValue = "general") String room, Model model) {
        var history = chatService.getHistory(room, 50);
        model.addAttribute("roomId",   room);
        model.addAttribute("history",  history);
        model.addAttribute("pageTitle","Staff Chat — " + room);
        return "chat/room";
    }

    // ─── WebSocket message handler ────────────────────────────────────────────

    @MessageMapping("/chat/{roomId}")
    @SendTo("/topic/chat/{roomId}")
    public ChatMessageResponse handleChatMessage(
            @DestinationVariable String roomId,
            ChatPayload payload) {
        var cu  = securityUtils.requireCurrentUser();
        var msg = chatService.save(roomId, cu.getId(), payload.getContent(), "TEXT");

        return ChatMessageResponse.builder()
                .id(msg.getId())
                .roomId(roomId)
                .senderId(cu.getId())
                .senderName(cu.getFullName())
                .content(msg.getContent())
                .messageType("TEXT")
                .sentAt(msg.getSentAt())
                .avatarInitials(initials(cu.getFullName()))
                .build();
    }

    @MessageMapping("/chat/{roomId}/typing")
    @SendTo("/topic/chat/{roomId}/typing")
    public Map<String, Object> handleTyping(
            @DestinationVariable String roomId,
            TypingPayload payload) {
        var cu = securityUtils.requireCurrentUser();
        return Map.of(
                "username", cu.getFullName(),
                "typing",   payload.isTyping(),
                "roomId",   roomId
        );
    }

    // ─── REST API for history ─────────────────────────────────────────────────

    @GetMapping("/api/chat/{roomId}/history")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> history(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(
                ApiResponse.ok(chatService.getHistory(roomId, limit)));
    }

    // ─── Payload inner types ──────────────────────────────────────────────────

    @Data public static class ChatPayload   { private String content; }
    @Data public static class TypingPayload { private boolean typing; }

    private String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.split(" ");
        return parts.length >= 2
                ? String.valueOf(parts[0].charAt(0)).toUpperCase()
                        + String.valueOf(parts[1].charAt(0)).toUpperCase()
                : name.substring(0, Math.min(2, name.length())).toUpperCase();
    }
}

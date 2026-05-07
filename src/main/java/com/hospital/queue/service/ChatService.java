package com.hospital.queue.service;

import com.hospital.queue.domain.entity.ChatMessage;
import com.hospital.queue.domain.entity.User;
import com.hospital.queue.dto.response.ChatMessageResponse;
import com.hospital.queue.exception.ResourceNotFoundException;
import com.hospital.queue.repository.ChatMessageRepository;
import com.hospital.queue.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository        userRepository;

    @Transactional
    public ChatMessage save(String roomId, Long senderId, String content, String type) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", senderId));

        ChatMessage msg = ChatMessage.builder()
                .roomId(roomId)
                .sender(sender)
                .senderName(sender.getFullName())
                .content(content)
                .messageType(type != null ? type : "TEXT")
                .build();

        return chatMessageRepository.save(msg);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getHistory(String roomId, int limit) {
        return chatMessageRepository
                .findRecentByRoomId(roomId, PageRequest.of(0, limit))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getFullHistory(String roomId) {
        return chatMessageRepository
                .findByRoomIdOrderBySentAtAsc(roomId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ChatMessageResponse toResponse(ChatMessage m) {
        String name = m.getSenderName();
        String initials = name.length() >= 2
                ? String.valueOf(name.charAt(0)).toUpperCase() +
                  String.valueOf(name.split(" ").length > 1
                          ? name.split(" ")[1].charAt(0) : name.charAt(1)).toUpperCase()
                : name.substring(0, 1).toUpperCase();

        return ChatMessageResponse.builder()
                .id(m.getId())
                .roomId(m.getRoomId())
                .senderId(m.getSender().getId())
                .senderName(m.getSenderName())
                .content(m.getContent())
                .messageType(m.getMessageType())
                .sentAt(m.getSentAt())
                .avatarInitials(initials)
                .build();
    }
}

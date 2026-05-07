package com.hospital.queue.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Builder
public class ChatMessageResponse {
    private Long          id;
    private String        roomId;
    private Long          senderId;
    private String        senderName;
    private String        content;
    private String        messageType;
    private LocalDateTime sentAt;
    private String        avatarInitials;
}

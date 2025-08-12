package com.connective.server.chat.presentation;

import com.connective.server.chat.domain.entity.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {
    
    private String roomId;
    private Long senderId;
    private String senderNickname;
    private String content;
    private Message.MessageType messageType;
    private LocalDateTime timestamp;
}
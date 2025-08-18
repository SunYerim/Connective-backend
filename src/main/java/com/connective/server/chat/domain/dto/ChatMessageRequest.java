package com.connective.server.chat.domain.dto;

import com.connective.server.chat.domain.entity.Message;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {
    
    private String content;
    private Message.MessageType messageType;
}
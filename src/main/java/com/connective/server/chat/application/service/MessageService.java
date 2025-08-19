package com.connective.server.chat.application.service;

import com.connective.server.chat.domain.dto.ChatMessageRequest;
import com.connective.server.chat.domain.dto.ChatMessageResponse;
import com.connective.server.chat.domain.entity.Message;

import java.util.List;

public interface MessageService {

    Message saveMessage(String roomId, Long senderId, ChatMessageRequest request);

    ChatMessageResponse createMessageResponse(Message message);

    List<Message> getMessageHistory(String roomId, int page, int size);

    Message getLatestMessage(String roomId);
}
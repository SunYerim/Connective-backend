package com.connective.server.chat.application.service;

import com.connective.server.chat.domain.dto.ChatMessageRequest;
import com.connective.server.chat.domain.dto.ChatMessageResponse;
import com.connective.server.chat.domain.entity.Message;
import com.connective.server.chat.domain.repository.MessageRepository;
import com.connective.server.user.domain.entity.User;
import com.connective.server.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Message saveMessage(String roomId, Long senderId, ChatMessageRequest request) {
        Long chatroomId = Long.parseLong(roomId);
        
        Message message = Message.builder()
                .chatroomId(chatroomId)
                .userId(senderId)
                .messageType(request.getMessageType())
                .messageContent(request.getContent())
                .build();

        Message savedMessage = messageRepository.save(message);
        log.info("Saved message: {} in room: {} from user: {}", 
                savedMessage.getMessageId(), roomId, senderId);
        
        return savedMessage;
    }

    @Override
    public ChatMessageResponse createMessageResponse(Message message) {
        User sender = userRepository.findById(message.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + message.getUserId()));

        return ChatMessageResponse.builder()
                .roomId(String.valueOf(message.getChatroomId()))
                .senderId(message.getUserId())
                .senderNickname(sender.getNickname())
                .content(message.getMessageContent())
                .messageType(message.getMessageType())
                .timestamp(message.getMessageSendAt())
                .build();
    }

    @Override
    public List<Message> getMessageHistory(String roomId, int page, int size) {
        Long chatroomId = Long.parseLong(roomId);
        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messagePage = messageRepository.findByChatroomIdOrderByMessageSendAtDesc(chatroomId, pageable);
        
        return messagePage.getContent();
    }

    @Override
    public Message getLatestMessage(String roomId) {
        Long chatroomId = Long.parseLong(roomId);
        return messageRepository.findLatestMessageByChatroomId(chatroomId);
    }
}
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
import java.util.Map;
import java.util.stream.Collectors;

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
            .orElseThrow(
                () -> new IllegalArgumentException("User not found: " + message.getUserId()));

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
        Page<Message> messagePage = messageRepository.findByChatroomIdOrderByMessageSendAtDesc(
            chatroomId, pageable);

        return messagePage.getContent();
    }

    public List<ChatMessageResponse> getMessageHistoryWithResponses(String roomId, int page,
        int size) {
        List<Message> messages = getMessageHistory(roomId, page, size);

        // 사용자 ID 목록 추출
        List<Long> userIds = messages.stream()
            .map(Message::getUserId)
            .distinct()
            .collect(Collectors.toList());

        // 사용자 정보를 한 번에 조회 (N+1 문제 해결)
        Map<Long, User> userMap = userRepository.findByIdIn(userIds).stream()
            .collect(Collectors.toMap(User::getId, user -> user));

        // ChatMessageResponse 생성
        return messages.stream()
            .map(message -> {
                User sender = userMap.get(message.getUserId());
                return ChatMessageResponse.builder()
                    .roomId(String.valueOf(message.getChatroomId()))
                    .senderId(message.getUserId())
                    .senderNickname(sender != null ? sender.getNickname() : "Unknown")
                    .content(message.getMessageContent())
                    .messageType(message.getMessageType())
                    .timestamp(message.getMessageSendAt())
                    .build();
            })
            .collect(Collectors.toList());
    }

    @Override
    public Message getLatestMessage(String roomId) {
        Long chatroomId = Long.parseLong(roomId);
        return messageRepository.findLatestMessageByChatroomId(chatroomId);
    }
}
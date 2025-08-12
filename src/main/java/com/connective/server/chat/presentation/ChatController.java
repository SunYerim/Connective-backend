package com.connective.server.chat.presentation;

import com.connective.server.chat.domain.entity.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat/{roomId}")
    public void sendMessage(@DestinationVariable String roomId, 
                           @Payload ChatMessageRequest request,
                           Authentication authentication) {
        
        Long userId = (Long) authentication.getPrincipal();
        log.info("Received message from user {} in room {}: {}", userId, roomId, request.getContent());
        
        // TODO: 메시지 저장 로직 (서비스 레이어)
        // Message savedMessage = chatService.saveMessage(roomId, userId, request);
        
        // 임시 응답 객체 생성
        ChatMessageResponse response = ChatMessageResponse.builder()
                .roomId(roomId)
                .senderId(userId)
                .senderNickname("User" + userId) // TODO: 실제 닉네임 조회
                .content(request.getContent())
                .messageType(request.getMessageType())
                .timestamp(LocalDateTime.now())
                .build();
        
        // 해당 채팅방 구독자들에게 메시지 전송
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, response);
        
        log.info("Message sent to room {}", roomId);
    }

    @MessageMapping("/chat/{roomId}/join")
    public void joinRoom(@DestinationVariable String roomId, 
                        Authentication authentication) {
        
        Long userId = (Long) authentication.getPrincipal();
        log.info("User {} joined room {}", userId, roomId);
        
        // 입장 메시지 생성
        ChatMessageResponse joinMessage = ChatMessageResponse.builder()
                .roomId(roomId)
                .senderId(userId)
                .senderNickname("User" + userId) // TODO: 실제 닉네임 조회
                .content("님이 입장하셨습니다.")
                .messageType(Message.MessageType.TEXT)
                .timestamp(LocalDateTime.now())
                .build();
        
        // 채팅방 구독자들에게 입장 알림
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, joinMessage);
    }

    @MessageMapping("/chat/{roomId}/leave")
    public void leaveRoom(@DestinationVariable String roomId, 
                         Authentication authentication) {
        
        Long userId = (Long) authentication.getPrincipal();
        log.info("User {} left room {}", userId, roomId);
        
        // 퇴장 메시지 생성
        ChatMessageResponse leaveMessage = ChatMessageResponse.builder()
                .roomId(roomId)
                .senderId(userId)
                .senderNickname("User" + userId) // TODO: 실제 닉네임 조회
                .content("님이 퇴장하셨습니다.")
                .messageType(Message.MessageType.TEXT)
                .timestamp(LocalDateTime.now())
                .build();
        
        // 채팅방 구독자들에게 퇴장 알림
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, leaveMessage);
    }
}
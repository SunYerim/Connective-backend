package com.connective.server.chat.application.service;

import com.connective.server.chat.domain.entity.ChatRoom;
import com.connective.server.chat.domain.entity.ChatRoomMember;
import com.connective.server.chat.domain.repository.ChatRoomMemberRepository;
import com.connective.server.chat.domain.repository.ChatRoomRepository;
import com.connective.server.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChatRoomServiceImpl implements ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ChatRoom createOrGetOneToOneChatRoom(Long userId1, Long userId2) {
        if (userId1.equals(userId2)) {
            throw new IllegalArgumentException("Cannot create chat room with yourself");
        }

        Optional<ChatRoom> existingRoom = chatRoomRepository.findOneToOneChatRoom(userId1, userId2);
        
        if (existingRoom.isPresent()) {
            ChatRoom chatRoom = existingRoom.get();
            if (chatRoom.getChatroomStatus() == ChatRoom.ChatRoomStatus.ARCHIVED) {
                chatRoom.updateStatus(ChatRoom.ChatRoomStatus.ACTIVE);
                log.info("Reactivated archived 1:1 chat room: {} for users: {}, {}", 
                        chatRoom.getChatroomId(), userId1, userId2);
            }
            return chatRoom;
        }

        validateUsersExist(userId1, userId2);

        ChatRoom chatRoom = ChatRoom.builder()
                .chatroomType(ChatRoom.ChatRoomType.ONE_TO_ONE)
                .chatroomStatus(ChatRoom.ChatRoomStatus.ACTIVE)
                .build();

        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);
        
        addMemberToChatRoom(savedChatRoom.getChatroomId(), userId1);
        addMemberToChatRoom(savedChatRoom.getChatroomId(), userId2);
        
        log.info("Created new 1:1 chat room: {} for users: {}, {}", 
                savedChatRoom.getChatroomId(), userId1, userId2);
        return savedChatRoom;
    }

    @Override
    @Transactional
    public void addMemberToChatRoom(Long chatroomId, Long userId) {
        if (chatRoomMemberRepository.existsByChatroomIdAndUserId(chatroomId, userId)) {
            return;
        }

        ChatRoomMember member = ChatRoomMember.builder()
                .chatroomId(chatroomId)
                .userId(userId)
                .build();

        chatRoomMemberRepository.save(member);
        log.info("Added user {} to chat room {}", userId, chatroomId);
    }

    @Override
    public ChatRoom getChatRoomById(Long chatroomId) {
        return chatRoomRepository.findById(chatroomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found: " + chatroomId));
    }

    @Override
    public List<ChatRoom> getChatRoomsByUserId(Long userId) {
        return chatRoomRepository.findChatRoomsByUserId(userId);
    }

    @Override
    public List<Long> getChatRoomMemberIds(Long chatroomId) {
        return chatRoomMemberRepository.findUserIdsByChatroomId(chatroomId);
    }

    @Override
    public boolean isMemberOfChatRoom(Long chatroomId, Long userId) {
        return chatRoomMemberRepository.existsByChatroomIdAndUserId(chatroomId, userId);
    }

    private void validateUsersExist(Long userId1, Long userId2) {
        if (!userRepository.existsById(userId1)) {
            throw new IllegalArgumentException("User not found: " + userId1);
        }
        if (!userRepository.existsById(userId2)) {
            throw new IllegalArgumentException("User not found: " + userId2);
        }
    }
}
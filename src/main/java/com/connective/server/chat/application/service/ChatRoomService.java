package com.connective.server.chat.application.service;

import com.connective.server.chat.domain.entity.ChatRoom;

import java.util.List;

public interface ChatRoomService {

    ChatRoom createOrGetOneToOneChatRoom(Long userId1, Long userId2);

    ChatRoom getChatRoomById(Long chatroomId);

    List<ChatRoom> getChatRoomsByUserId(Long userId);

    List<Long> getChatRoomMemberIds(Long chatroomId);

    boolean isMemberOfChatRoom(Long chatroomId, Long userId);

    void addMemberToChatRoom(Long chatroomId, Long userId);
}
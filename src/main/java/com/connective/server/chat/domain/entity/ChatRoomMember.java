package com.connective.server.chat.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ChatRoomMember")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chatroom_member_id")
    private Long chatroomMemberId;

    @Column(name = "chatroom_id", nullable = false)
    private Long chatroomId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @Builder
    public ChatRoomMember(Long chatroomId, Long userId, Long lastReadMessageId) {
        this.chatroomId = chatroomId;
        this.userId = userId;
        this.lastReadMessageId = lastReadMessageId;
    }

    // 비즈니스 메소드
    public void updateLastReadMessage(Long messageId) {
        this.lastReadMessageId = messageId;
    }
}
package com.connective.server.chat.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "ChatRoom")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chatroom_id")
    private Long chatroomId;

    @Enumerated(EnumType.STRING)
    @Column(name = "chatroom_type", nullable = false)
    private ChatRoomType chatroomType;

    @Column(name = "chatroom_name")
    private String chatroomName;

    @Enumerated(EnumType.STRING)
    @Column(name = "chatroom_status", nullable = false)
    private ChatRoomStatus chatroomStatus;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public ChatRoom(ChatRoomType chatroomType, String chatroomName, ChatRoomStatus chatroomStatus) {
        this.chatroomType = chatroomType;
        this.chatroomName = chatroomName;
        this.chatroomStatus = chatroomStatus != null ? chatroomStatus : ChatRoomStatus.ACTIVE;
    }

    public enum ChatRoomType {
        ONE_TO_ONE,  // 1:1 채팅
        GROUP        // 그룹 채팅
    }

    public enum ChatRoomStatus {
        ACTIVE,    // 활성 상태
        ARCHIVED,  // 보관됨
        DELETED    // 삭제됨
    }

    // 비즈니스 메소드
    public void updateStatus(ChatRoomStatus status) {
        this.chatroomStatus = status;
    }

    public void updateName(String name) {
        this.chatroomName = name;
    }
}
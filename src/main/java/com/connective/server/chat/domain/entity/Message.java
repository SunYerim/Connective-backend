package com.connective.server.chat.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "Message")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "chatroom_id", nullable = false)
    private Long chatroomId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType;

    @Column(name = "message_content")
    private String messageContent;

    @CreatedDate
    @Column(name = "message_send_at", nullable = false)
    private LocalDateTime messageSendAt;

    @Builder
    public Message(Long chatroomId, Long userId, MessageType messageType, String messageContent) {
        this.chatroomId = chatroomId;
        this.userId = userId;
        this.messageType = messageType;
        this.messageContent = messageContent;
    }

    public enum MessageType {
        TEXT,   // 텍스트 메시지
        IMAGE,  // 이미지 파일
        FILE    // 일반 파일
    }
}
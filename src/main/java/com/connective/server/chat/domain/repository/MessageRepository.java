package com.connective.server.chat.domain.repository;

import com.connective.server.chat.domain.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // 특정 채팅방의 메시지 조회 (페이징)
    Page<Message> findByChatroomIdOrderByMessageSendAtDesc(Long chatroomId, Pageable pageable);

    // 특정 채팅방의 최근 메시지 조회
    @Query("SELECT m FROM Message m WHERE m.chatroomId = :chatroomId ORDER BY m.messageSendAt DESC LIMIT 1")
    Message findLatestMessageByChatroomId(@Param("chatroomId") Long chatroomId);

    // 특정 사용자가 보낸 메시지 개수
    long countByUserId(Long userId);

    // 특정 채팅방의 메시지 개수
    long countByChatroomId(Long chatroomId);
}
package com.connective.server.chat.domain.repository;

import com.connective.server.chat.domain.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 특정 사용자가 참여한 채팅방 목록 조회
    @Query("SELECT cr FROM ChatRoom cr " +
           "JOIN ChatRoomMember crm ON cr.chatroomId = crm.chatroomId " +
           "WHERE crm.userId = :userId AND cr.chatroomStatus = 'ACTIVE' " +
           "ORDER BY cr.updatedAt DESC")
    List<ChatRoom> findChatRoomsByUserId(@Param("userId") Long userId);

    // 1:1 채팅방 존재 여부 확인
    @Query("SELECT cr FROM ChatRoom cr " +
           "WHERE cr.chatroomType = 'ONE_TO_ONE' " +
           "AND cr.chatroomId IN (" +
           "    SELECT crm1.chatroomId FROM ChatRoomMember crm1 " +
           "    WHERE crm1.userId = :userId1 " +
           "    AND crm1.chatroomId IN (" +
           "        SELECT crm2.chatroomId FROM ChatRoomMember crm2 " +
           "        WHERE crm2.userId = :userId2" +
           "    )" +
           ")")
    Optional<ChatRoom> findOneToOneChatRoom(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    // 채팅방 상태별 조회
    List<ChatRoom> findByChatroomStatusOrderByUpdatedAtDesc(ChatRoom.ChatRoomStatus status);
}
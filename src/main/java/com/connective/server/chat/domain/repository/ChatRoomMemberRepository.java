package com.connective.server.chat.domain.repository;

import com.connective.server.chat.domain.entity.ChatRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    // 특정 채팅방의 멤버 목록 조회
    List<ChatRoomMember> findByChatroomId(Long chatroomId);

    // 특정 사용자의 채팅방 멤버십 조회
    List<ChatRoomMember> findByUserId(Long userId);

    // 특정 채팅방의 특정 사용자 멤버십 조회
    Optional<ChatRoomMember> findByChatroomIdAndUserId(Long chatroomId, Long userId);

    // 특정 채팅방의 멤버 수 조회
    long countByChatroomId(Long chatroomId);

    // 특정 사용자가 참여한 채팅방 수 조회
    long countByUserId(Long userId);

    // 특정 채팅방에서 사용자 존재 여부 확인
    boolean existsByChatroomIdAndUserId(Long chatroomId, Long userId);

    // 특정 채팅방의 사용자 ID 목록 조회
    @Query("SELECT crm.userId FROM ChatRoomMember crm WHERE crm.chatroomId = :chatroomId")
    List<Long> findUserIdsByChatroomId(@Param("chatroomId") Long chatroomId);
}
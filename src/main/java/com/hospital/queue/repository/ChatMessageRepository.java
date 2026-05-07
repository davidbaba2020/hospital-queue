package com.hospital.queue.repository;

import com.hospital.queue.domain.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("""
           SELECT m FROM ChatMessage m
           WHERE m.roomId = :roomId
           ORDER BY m.sentAt ASC
           """)
    List<ChatMessage> findByRoomIdOrderBySentAtAsc(@Param("roomId") String roomId);

    @Query("""
           SELECT m FROM ChatMessage m
           JOIN FETCH m.sender
           WHERE m.roomId = :roomId
           ORDER BY m.sentAt DESC
           """)
    List<ChatMessage> findRecentByRoomId(@Param("roomId") String roomId, Pageable pageable);
}

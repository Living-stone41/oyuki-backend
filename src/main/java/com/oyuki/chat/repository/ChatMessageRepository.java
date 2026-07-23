package com.oyuki.chat.repository;

import com.oyuki.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage,Long> {
    @Query("select m from ChatMessage m where (m.sender.id=:a and m.recipient.id=:b) or (m.sender.id=:b and m.recipient.id=:a) order by m.createdAt")
    List<ChatMessage> conversation(@Param("a") Long a, @Param("b") Long b);

    @Query("select m from ChatMessage m where m.sender.id=:userId or m.recipient.id=:userId order by m.createdAt desc")
    List<ChatMessage> allForUser(@Param("userId") Long userId);

    List<ChatMessage> findAllByRecipient_IdAndReadFlagFalse(Long id);
}

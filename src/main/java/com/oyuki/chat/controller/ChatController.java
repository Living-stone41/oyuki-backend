package com.oyuki.chat.controller;

import com.oyuki.chat.entity.ChatMessage;
import com.oyuki.chat.repository.ChatMessageRepository;
import com.oyuki.user.entity.User;
import com.oyuki.user.repository.UserRepository;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatMessageRepository repo;
    private final UserRepository users;

    public ChatController(ChatMessageRepository repo, UserRepository users) {
        this.repo = repo; this.users = users;
    }

    @PostMapping("/messages")
    public ResponseEntity<?> send(Authentication authentication, @RequestBody Map<String,String> body) {
        Long senderId = (Long) authentication.getPrincipal();
        Long recipientId;
        try { recipientId = Long.valueOf(body.get("recipientId")); }
        catch (Exception ex) { throw new IllegalArgumentException("Choose a valid recipient"); }
        if (senderId.equals(recipientId)) throw new IllegalArgumentException("You cannot message yourself");
        String text = body.get("message") == null ? "" : body.get("message").trim();
        if (text.isBlank()) throw new IllegalArgumentException("Message cannot be empty");
        if (text.length() > 3000) throw new IllegalArgumentException("Message is too long");
        ChatMessage message = ChatMessage.builder()
                .sender(users.findById(senderId).orElseThrow())
                .recipient(users.findById(recipientId).orElseThrow())
                .message(text).build();
        return ResponseEntity.status(HttpStatus.CREATED).body(map(repo.save(message)));
    }

    @GetMapping("/with/{userId}")
    public List<Map<String,Object>> conversation(Authentication authentication, @PathVariable Long userId) {
        Long me = (Long) authentication.getPrincipal();
        users.findById(userId).orElseThrow();
        List<ChatMessage> list = repo.conversation(me, userId);
        list.stream().filter(x -> x.getRecipient().getId().equals(me)).forEach(x -> x.setReadFlag(true));
        repo.saveAll(list);
        return list.stream().map(this::map).toList();
    }

    @GetMapping("/conversations")
    public List<Map<String,Object>> conversations(Authentication authentication) {
        Long me = (Long) authentication.getPrincipal();
        LinkedHashMap<Long, ChatMessage> latest = new LinkedHashMap<>();
        for (ChatMessage message : repo.allForUser(me)) {
            Long otherId = message.getSender().getId().equals(me)
                    ? message.getRecipient().getId() : message.getSender().getId();
            latest.putIfAbsent(otherId, message);
        }
        return latest.entrySet().stream().map(entry -> {
            ChatMessage message = entry.getValue();
            User other = message.getSender().getId().equals(me) ? message.getRecipient() : message.getSender();
            long unread = repo.findAllByRecipient_IdAndReadFlagFalse(me).stream()
                    .filter(m -> m.getSender().getId().equals(other.getId())).count();
            Map<String,Object> result = new LinkedHashMap<>();
            result.put("userId", other.getId());
            result.put("name", other.getFullName());
            result.put("role", other.getRole());
            result.put("lastMessage", message.getMessage());
            result.put("createdAt", message.getCreatedAt());
            result.put("unread", unread);
            return result;
        }).toList();
    }

    @GetMapping("/unread")
    public Map<String,Object> unread(Authentication authentication) {
        return Map.of("count", repo.findAllByRecipient_IdAndReadFlagFalse((Long) authentication.getPrincipal()).size());
    }

    private Map<String,Object> map(ChatMessage m) {
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("id",m.getId()); result.put("senderId",m.getSender().getId());
        result.put("senderName",m.getSender().getFullName()); result.put("recipientId",m.getRecipient().getId());
        result.put("recipientName",m.getRecipient().getFullName()); result.put("message",m.getMessage());
        result.put("read",m.isReadFlag()); result.put("createdAt",m.getCreatedAt());
        return result;
    }
}

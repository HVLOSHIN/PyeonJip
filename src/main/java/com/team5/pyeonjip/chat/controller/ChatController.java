package com.team5.pyeonjip.chat.controller;

import com.team5.pyeonjip.chat.dto.ChatMessageDto;
import com.team5.pyeonjip.chat.dto.ChatRoomDto;
import com.team5.pyeonjip.chat.service.ChatMessageService;
import com.team5.pyeonjip.chat.service.ChatRoomService;
import com.team5.pyeonjip.user.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    // userId에 따른 채팅이력 리스트
    @GetMapping("/chat-room-list/{email}")
    public ResponseEntity<List<ChatRoomDto>> getChatRooms(@PathVariable("email") String email){
        List<ChatRoomDto> chatRoom = chatRoomService.getChatRoomsByUserEmail(email);

        return ResponseEntity.ok().body(chatRoom);
    }

    // 대기 상태의 채팅방 생성
    @PostMapping("/waiting-room")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ChatRoomDto> createWaitingRoom(@RequestBody ChatRoomDto chatRoomDto, Authentication authentication) {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String userEmail = userDetails.getUsername(); // CustomUserDetails에서는 getUsername()이 이메일을 반환합니다.

        // userId 대신 userEmail을 사용하도록 ChatRoomService 메서드를 수정해야 할 수 있습니다.
        ChatRoomDto createdChatRoom = chatRoomService.createWaitingRoom(chatRoomDto.getCategory(), userEmail);
        return ResponseEntity.ok(createdChatRoom);
    }

    // 관리자용: 채팅방 활성화
    @PostMapping("/activate-room/{chatRoomId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ChatRoomDto> activateChatRoom(@PathVariable("chatRoomId") Long chatRoomId, Authentication authentication) {
        CustomUserDetails admin = (CustomUserDetails) authentication.getPrincipal();
        String adminEmail = admin.getUsername(); // CustomUserDetails에서는 getUsername()이 이메일을 반환합니다.

        ChatRoomDto activatedRoom = chatRoomService.activateChatRoom(chatRoomId, adminEmail);
        return ResponseEntity.ok(activatedRoom);
    }

    @PostMapping("/close-room/{chatRoomId}")
    public ResponseEntity<?> closeChatRoom(@PathVariable("chatRoomId") Long chatRoomId) {
        try {
            ChatRoomDto closedRoom = chatRoomService.closeChatRoom(chatRoomId);
            messagingTemplate.convertAndSend("/topic/chat-room-closed/" + chatRoomId, closedRoom);
            return ResponseEntity.ok(closedRoom);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to close chat room: " + e.getMessage());
        }
    }

    @GetMapping("/waiting-rooms")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ChatRoomDto>> getWaitingRooms() {
        List<ChatRoomDto> waitingRooms = chatRoomService.getWaitingChatRooms();
        return ResponseEntity.ok(waitingRooms);
    }

    @GetMapping("/chat-room/{chatRoomId}")
    public ResponseEntity<ChatRoomDto> getChatRoom(@PathVariable("chatRoomId") Long chatRoomId) {
        ChatRoomDto chatRoom = chatRoomService.getChatRoomById(chatRoomId);
        return ResponseEntity.ok(chatRoom);
    }

    // 채팅방에 따른 채팅 메시지 조회
    @GetMapping("/chat-message-history/{chatRoomId}")
    public ResponseEntity<List<ChatMessageDto>> getChatMessages(@PathVariable("chatRoomId") Long chatRoomId){
        List<ChatMessageDto> chatMessage = chatMessageService.getChatMessagesByChatRoomId(chatRoomId);

        return ResponseEntity.ok().body(chatMessage);
    }
}

package com.nafay.skribbl.config;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.nafay.skribbl.enums.MessageType;
import com.nafay.skribbl.model.ChatMessage;
import com.nafay.skribbl.model.Room;
import com.nafay.skribbl.model.RoomUpdateMessage;
import com.nafay.skribbl.service.RoomManagerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {
  private final SimpMessageSendingOperations messageTemplate;
  private final RoomManagerService roomManagerService;

  @EventListener
  public void handleWebSocketDisconnectEvent(SessionDisconnectEvent event) {
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
    String username = (String) headerAccessor.getSessionAttributes().get("username");
    String roomId = (String) headerAccessor.getSessionAttributes().get("roomId");
    
    if (username != null && roomId != null) {
      log.info("User disconnected: {} from room: {}", username, roomId);
      
      roomManagerService.leaveRoom(roomId, username);
      Room room = roomManagerService.getRoom(roomId);
      
      if (room != null) {
        // Broadcast room update to others
        messageTemplate.convertAndSend("/topic/room/" + roomId, 
          RoomUpdateMessage.builder().type(MessageType.ROOM_UPDATE).room(room).build());
      }
      
      var chatMessage = ChatMessage.builder()
        .username(username)
        .type(MessageType.LEAVE)
        .content(username + " left the room")
        .build();
      messageTemplate.convertAndSend("/topic/room/" + roomId, chatMessage);
    }
  }
}

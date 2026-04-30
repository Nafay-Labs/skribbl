package com.nafay.skribbl.controller;

import java.util.UUID;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import com.nafay.skribbl.enums.MessageType;
import com.nafay.skribbl.exception.PlayerNotFoundException;
import com.nafay.skribbl.model.ChatMessage;
import com.nafay.skribbl.model.DrawEvent;
import com.nafay.skribbl.model.Player;
import com.nafay.skribbl.model.Room;
import com.nafay.skribbl.model.RoomUpdateMessage;
import com.nafay.skribbl.enums.GameState;
import com.nafay.skribbl.repository.PlayerRepository;
import com.nafay.skribbl.service.RoomManagerService;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import lombok.AllArgsConstructor;

@Controller
@AllArgsConstructor
public class RoomController {
  private RoomManagerService roomManagerService;
  private PlayerRepository playerRepository;
  private SimpMessagingTemplate messagingTemplate;
  
  @MessageMapping("/room/{roomId}/join")
  @SendTo("/topic/room/{roomId}")
  public ChatMessage joinRoom(@Payload UUID playerId, SimpMessageHeaderAccessor headerAccessor, @DestinationVariable String roomId) {
    Player player = roomManagerService.joinRoom(roomId, playerId);

    if (player != null) {
      headerAccessor.getSessionAttributes().put("roomId", roomId);
      headerAccessor.getSessionAttributes().put("username", player.getNickname());

      player.setSessionID(headerAccessor.getSessionId());
      playerRepository.save(player);
      
      Room room = roomManagerService.getRoom(roomId);
      if (room != null) {
        if (room.getCurrentRoundStrokes() != null && !room.getCurrentRoundStrokes().isEmpty()) {
          messagingTemplate.convertAndSendToUser(headerAccessor.getSessionId(), "/queue/room/strokes", room.getCurrentRoundStrokes(), headerAccessor.getMessageHeaders());
        }
        
        // Send initial room state to the joined player
        messagingTemplate.convertAndSendToUser(headerAccessor.getSessionId(), "/queue/room/update", 
          RoomUpdateMessage.builder().type(MessageType.ROOM_UPDATE).room(room).build(), 
          headerAccessor.getMessageHeaders());
          
        // Notify others about player list change
        messagingTemplate.convertAndSend( "/topic/room/" + roomId, 
          RoomUpdateMessage.builder().type(MessageType.ROOM_UPDATE).room(room).build());
      }

      return ChatMessage.builder()
        .type(MessageType.JOIN_GAME)
        .content(player.getNickname() + " joined the room")
        .username(player.getNickname())
        .build();
    }

    return ChatMessage.builder()
      .type(MessageType.JOIN_GAME)
      .content("Failed to join the room")
      .username("System")
      .build();
  }

  @MessageMapping("room/create")
  @SendToUser("/queue/room/create")
  public ChatMessage createRoom(@Payload UUID playerId, SimpMessageHeaderAccessor headerAccessor) {
    Player player = playerRepository.findByPlayerId(playerId).orElseThrow(() -> new PlayerNotFoundException("Player not found"));
    Room room = roomManagerService.createRoom(player);

    headerAccessor.getSessionAttributes().put("roomId", room.getRoomID());
    headerAccessor.getSessionAttributes().put("username", player.getNickname());

    player.setSessionID(headerAccessor.getSessionId());
    playerRepository.save(player);

    return ChatMessage.builder()
    .type(MessageType.JOIN_GAME)
    .content("Room created successfully. ID: " + room.getRoomID())
    .username("System")
    .build();
  }

  @MessageMapping("/room/{roomId}/draw")
  @SendTo("/topic/room/{roomId}")
  public DrawEvent draw(@Payload DrawEvent drawEvent, @DestinationVariable String roomId, SimpMessageHeaderAccessor headerAccessor) {
    Room room = roomManagerService.getRoom(roomId);
    if (room != null && room.getState() == GameState.DRAWING) {
      Player currentDrawer = room.getCurrentDrawer();
      if (currentDrawer != null && headerAccessor.getSessionId().equals(currentDrawer.getSessionID())) {
        if (room.getCurrentRoundStrokes() != null) {
          room.getCurrentRoundStrokes().add(drawEvent);
        }
        return drawEvent;
      }
    }
    return null;
  }

  @MessageMapping("/room/{roomId}/start")
  @SendTo("/topic/room/{roomId}")
  public RoomUpdateMessage startGame(@DestinationVariable String roomId, SimpMessageHeaderAccessor headerAccessor) {
    Room room = roomManagerService.getRoom(roomId);
    if (room != null && room.getState() == GameState.LOBBY) {
      Player admin = room.getAdmin();
      if (admin != null && headerAccessor.getSessionId().equals(admin.getSessionID())) {
        if (!room.getPlayers().isEmpty()) {
          room.setState(GameState.DRAWING);
          room.setCurrentDrawer(room.getPlayers().get(0));
          room.setCurrentRound(1);
          room.setMaxRounds(3); // Default
          
          return RoomUpdateMessage.builder()
            .type(MessageType.ROOM_UPDATE)
            .room(room)
            .build();
        }
      }
    }
    return null;
  }
}

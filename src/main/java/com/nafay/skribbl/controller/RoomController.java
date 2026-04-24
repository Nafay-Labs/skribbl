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
import com.nafay.skribbl.model.Player;
import com.nafay.skribbl.model.Room;
import com.nafay.skribbl.repository.PlayerRepository;
import com.nafay.skribbl.service.RoomManagerService;

import lombok.AllArgsConstructor;

@Controller
@AllArgsConstructor
public class RoomController {
  private RoomManagerService roomManagerService;
  private PlayerRepository playerRepository;
  
  @MessageMapping("/room/{roomId}/join")
  @SendTo("/topic/room/{roomId}")
  public ChatMessage joinRoom(@Payload UUID playerId, SimpMessageHeaderAccessor headerAccessor, @DestinationVariable String roomId) {
    Player player = roomManagerService.joinRoom(roomId, playerId);

    if (player != null) {
      headerAccessor.getSessionAttributes().put("roomId", roomId);
      headerAccessor.getSessionAttributes().put("username", player.getNickname());

      player.setSessionID(headerAccessor.getSessionId());
      playerRepository.save(player);
      
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
}

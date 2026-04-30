package com.nafay.skribbl.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.nafay.skribbl.enums.GameState;
import com.nafay.skribbl.exception.PlayerAlreadyInRoomException;
import com.nafay.skribbl.exception.PlayerNotFoundException;
import com.nafay.skribbl.model.Player;
import com.nafay.skribbl.model.Room;
import com.nafay.skribbl.repository.PlayerRepository;

@Service
public class RoomManagerService {
  private ConcurrentHashMap<String, Room> manager;
  private PlayerRepository playerRepository;

  public RoomManagerService(PlayerRepository playerRepository) {
    this.manager = new ConcurrentHashMap<>();
    this.playerRepository = playerRepository;
  }

  public Player joinRoom(String roomId, UUID playerId) {
    Player player = playerRepository.findByPlayerId(playerId).orElseThrow(() -> new PlayerNotFoundException("Player not found"));

    Room room = manager.get(roomId);
    if (room != null && room.getState().equals(GameState.LOBBY)) {
      if (room.getPlayers().contains(player)) {
        return player;
      }
      if (room.getPlayers().size() < 10) {
        room.getPlayers().add(player);
        return player;
      }
    }

    return null;
  }

  public Room createRoom(Player player) {
    if (player.getSessionID() == null) {
      String roomId = UUID.randomUUID().toString();
      Room room = Room.builder()
        .roomID(roomId)
        .players(new ArrayList<>(List.of(player)))
        .admin(player)
        .state(GameState.LOBBY)
        .currentRoundStrokes(new ArrayList<>())
        .build();
      manager.put(roomId, room);
      return room;
    }

    throw new PlayerAlreadyInRoomException("Player is already in a room");
  }

  public void leaveRoom(String roomId, String username) {
    Room room = manager.get(roomId);
    if (room != null) {
      room.getPlayers().removeIf(p -> p.getNickname().equals(username));
      
      // If room is empty, remove it
      if (room.getPlayers().isEmpty()) {
        manager.remove(roomId);
      }
    }
  }

  public Room getRoom(String roomId) {
    return manager.get(roomId);
  }
}

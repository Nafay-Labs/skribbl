package com.nafay.skribbl.model;

import java.util.UUID;

import lombok.Data;

@Data
public class Player {
  private UUID playerId;
  private String nickname;
  private String sessionID; // WebSocket session
  private int score;
  private Boolean hasGuessedCorrectly; // boolean for the current round
}

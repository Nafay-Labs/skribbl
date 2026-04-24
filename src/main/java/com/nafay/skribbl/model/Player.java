package com.nafay.skribbl.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

import lombok.Data;

@Data
@Entity
@Table(name = "players")
public class Player {
  @Id
  private UUID playerId;
  private String nickname;
  private String sessionID; // WebSocket session
  private int score;
  private Boolean hasGuessedCorrectly; // boolean for the current round
}

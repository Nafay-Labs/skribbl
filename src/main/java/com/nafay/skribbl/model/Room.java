package com.nafay.skribbl.model;

import java.util.List;

import com.nafay.skribbl.enums.GameState;

import lombok.Data;

@Data
public class Room {
  private int roomID;
  private List<Player> players;
  private GameState state;
  private int currentRound;
  private int maxRounds;
  private Player currentDrawer;
  private String currentWord;
  private Long turnEndTime;
  private List<String> wordHistory;
}

package com.nafay.skribbl.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nafay.skribbl.model.Player;
import com.nafay.skribbl.repository.PlayerRepository;

import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
public class PlayerController {
  private PlayerRepository playerRepository;

  @PostMapping("/api/player/register")
  public Player registerPlayer(@RequestParam String nickname) {
    Player player = new Player();
    player.setPlayerId(UUID.randomUUID());
    player.setNickname(nickname);
    player.setScore(0);
    player.setHasGuessedCorrectly(false);
    return playerRepository.save(player);
  }
}

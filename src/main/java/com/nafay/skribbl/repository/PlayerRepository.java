package com.nafay.skribbl.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nafay.skribbl.model.Player;

public interface PlayerRepository extends JpaRepository<Player, UUID> {
  Optional<Player> findByPlayerId(UUID playerId);
}

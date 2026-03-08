package com.nafay.skribbl.model;

import java.util.UUID;

import com.nafay.skribbl.enums.MessageType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatMessage {
  private UUID playerId;
  private String username;
  private String content;
  private MessageType type;
}

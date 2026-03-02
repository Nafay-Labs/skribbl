package com.nafay.skribbl.model;

import java.util.UUID;

import com.nafay.skribbl.enums.MessageType;

import lombok.Data;

@Data
public class ChatMessage {
  private UUID playerId;
  private String nickname;
  private String content;
  private MessageType type;
}

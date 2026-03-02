package com.nafay.skribbl.model;

import java.util.UUID;

import com.nafay.skribbl.enums.MessageType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatMessage {
  private UUID playerId;
  private String nickname;
  private String content;
  private MessageType type;
}

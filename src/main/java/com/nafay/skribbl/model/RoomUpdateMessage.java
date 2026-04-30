package com.nafay.skribbl.model;

import com.nafay.skribbl.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoomUpdateMessage {
    private MessageType type;
    private Room room;
}

package com.nafay.skribbl.exception;

public class PlayerAlreadyInRoomException extends RuntimeException {
    public PlayerAlreadyInRoomException(String message) {
        super(message);
    }
}

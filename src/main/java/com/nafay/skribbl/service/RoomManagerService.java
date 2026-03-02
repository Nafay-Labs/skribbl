package com.nafay.skribbl.service;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.nafay.skribbl.model.Room;

@Service
public class RoomManagerService {
  private ConcurrentHashMap<String, Room> manager;

  
}

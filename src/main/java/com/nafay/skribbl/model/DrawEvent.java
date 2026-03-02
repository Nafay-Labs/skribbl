package com.nafay.skribbl.model;

import com.nafay.skribbl.enums.DrawEventType;

import lombok.Data;

@Data
public class DrawEvent {
  private DrawEventType type;
  private int x1, y1, x2, y2;
  private String color;
  private int brushSize;
}

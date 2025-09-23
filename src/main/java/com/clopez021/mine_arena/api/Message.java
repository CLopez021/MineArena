package com.clopez021.mine_arena.api;

import com.google.gson.JsonObject;
import java.util.Objects;

public class Message {
  private final String role;
  private final String content;

  public Message(String role, String content) {
    this.role = Objects.requireNonNull(role, "role");
    this.content = Objects.requireNonNull(content, "content");
  }

  public JsonObject toJsonObject() {
    JsonObject obj = new JsonObject();
    obj.addProperty("role", role);
    obj.addProperty("content", content);
    return obj;
  }
}

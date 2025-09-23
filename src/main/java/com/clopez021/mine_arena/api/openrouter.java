package com.clopez021.mine_arena.api;

import static com.clopez021.mine_arena.ServerConfig.openrouterApiKey;
import static com.clopez021.mine_arena.ServerConfig.openrouterModel;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class openrouter {
  /**
   * Calls OpenRouter chat completions API with a single user message and returns assistant content.
   */
  public static String chat(String message) {
    return chat(List.of(new Message("user", message)));
  }

  /**
   * Calls OpenRouter chat completions API with a list of role/content messages.
   *
   * @param messagesList Ordered list of messages to send
   * @return Assistant reply content as plain text
   */
  public static String chat(List<Message> messagesList) {
    try {
      JsonObject body = new JsonObject();
      body.addProperty("model", openrouterModel);
      JsonArray messages = new JsonArray();
      for (Message m : messagesList) {
        messages.add(m.toJsonObject());
      }
      body.add("messages", messages);

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create("https://openrouter.ai/api/v1/chat/completions"))
              .header("Content-Type", "application/json")
              .header("Authorization", "Bearer " + openrouterApiKey)
              .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
              .build();

      HttpResponse<String> response =
          HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() >= 400) {
        try {
          JsonObject err = JsonParser.parseString(response.body()).getAsJsonObject();
          String msg = err.has("error") ? err.get("error").getAsString() : response.body();
          throw new RuntimeException("OpenRouter error: " + msg);
        } catch (Exception ignore) {
          throw new RuntimeException(
              "OpenRouter HTTP " + response.statusCode() + ": " + response.body());
        }
      }

      JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
      if (!json.has("choices") || json.getAsJsonArray("choices").size() == 0) {
        throw new RuntimeException("No choices returned by OpenRouter");
      }
      JsonObject choice = json.getAsJsonArray("choices").get(0).getAsJsonObject();
      JsonObject assistantMsg = choice.getAsJsonObject("message");
      return assistantMsg.get("content").getAsString();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}

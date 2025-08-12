package com.clopez021.mine_arena.audio;

import com.clopez021.mine_arena.Config;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

public class LLMService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final Gson gson = new Gson();

    /**
     * Transcribes audio using OpenRouter API with a transcription model
     * @param base64Audio Base64 encoded MP3 audio data
     * @param playerName Name of the player for context
     * @return CompletableFuture with transcription result
     */
    public static CompletableFuture<String> transcribeAudio(String base64Audio, String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (Config.openrouterApiKey == null || Config.openrouterApiKey.isEmpty() || Config.openrouterApiKey.equals("YOUR_OPENROUTER_API_KEY")) {
                    throw new RuntimeException("OpenRouter API key not configured. Please set it in the mod config file.");
                }

                // Create the request payload
                JsonObject payload = createTranscriptionPayload(base64Audio, playerName);
                
                // Make HTTP request
                String response = makeHttpRequest(payload);
                
                // Parse response and extract transcription
                return parseTranscriptionResponse(response);
                
            } catch (Exception e) {
                LOGGER.error("Failed to transcribe audio for player {}: {}", playerName, e.getMessage());
                return "Transcription failed: " + e.getMessage();
            }
        });
    }

    /**
     * Creates the JSON payload for OpenRouter transcription request
     */
    private static JsonObject createTranscriptionPayload(String base64Audio, String playerName) {
        JsonObject payload = new JsonObject();
        payload.addProperty("model", "google/gemini-2.5-flash");
        
        JsonArray messages = new JsonArray();
        
        // User message with audio
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        
        JsonArray content = new JsonArray();
        
        // Text part
        JsonObject textPart = new JsonObject();
        textPart.addProperty("type", "text");
        textPart.addProperty("text", "Please transcribe this audio file.");
        content.add(textPart);
        
        // Audio part
        JsonObject audioPart = new JsonObject();
        audioPart.addProperty("type", "input_audio");
        JsonObject inputAudio = new JsonObject();
        inputAudio.addProperty("data", base64Audio);
        inputAudio.addProperty("format", "mp3");
        audioPart.add("input_audio", inputAudio);
        content.add(audioPart);
        
        userMessage.add("content", content);
        messages.add(userMessage);
        
        payload.add("messages", messages);
        
        return payload;
    }

    /**
     * Makes HTTP request to OpenRouter API
     */
    private static String makeHttpRequest(JsonObject payload) throws Exception {
        URL url = new URL(OPENROUTER_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        // Set essential request properties only
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + Config.openrouterApiKey);
        connection.setDoOutput(true);
        
        // Send request
        try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream())) {
            writer.write(gson.toJson(payload));
            writer.flush();
        }
        
        // Read response
        int responseCode = connection.getResponseCode();
        BufferedReader reader;
        
        if (responseCode >= 200 && responseCode < 300) {
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        } else {
            reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
        }
        
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        if (responseCode >= 200 && responseCode < 300) {
            return response.toString();
        } else {
            throw new RuntimeException("HTTP " + responseCode + ": " + response.toString());
        }
    }

    /**
     * Parses the OpenRouter response and extracts transcription text
     */
    private static String parseTranscriptionResponse(String response) {
        try {
            JsonObject responseObj = gson.fromJson(response, JsonObject.class);
            
            if (responseObj.has("choices")) {
                JsonArray choices = responseObj.getAsJsonArray("choices");
                if (choices.size() > 0) {
                    JsonObject firstChoice = choices.get(0).getAsJsonObject();
                    if (firstChoice.has("message")) {
                        JsonObject message = firstChoice.getAsJsonObject("message");
                        if (message.has("content")) {
                            return message.get("content").getAsString().trim();
                        }
                    }
                }
            }
            
            // If we can't parse the expected format, return the raw response
            LOGGER.warn("Unexpected response format from OpenRouter: {}", response);
            return "Transcription completed, but response format was unexpected.";
            
        } catch (Exception e) {
            LOGGER.error("Failed to parse transcription response: {}", e.getMessage());
            return "Failed to parse transcription response.";
        }
    }

    /**
     * Checks if the LLM service is properly configured
     */
    public static boolean isConfigured() {
        return Config.openrouterApiKey != null && !Config.openrouterApiKey.isEmpty() && !Config.openrouterApiKey.equals("YOUR_OPENROUTER_API_KEY");
    }

    /**
     * Gets configuration instructions for the user
     */
    public static String getConfigurationInstructions() {
        return "To use transcription, set your OpenRouter API key in the mod config file at config/mine_arena-common.toml.";
    }
} 
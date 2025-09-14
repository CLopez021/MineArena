package com.clopez021.mine_arena.speech_recognition;

import com.clopez021.mine_arena.packets.PacketHandler;
import com.clopez021.mine_arena.packets.VoiceSidecarConfigPacket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * Manager class for handling speech recognition functionality. This class focuses solely on speech
 * recognition operations. Player management is handled by PlayerManager.
 */
public class SpeechRecognitionManager {

  // Track which players have active voice recognition
  private static final Set<UUID> activeVoiceRecognition = ConcurrentHashMap.newKeySet();

  /**
   * Starts voice recognition for a player with specified mapping and language.
   *
   * @param player The ServerPlayer to start voice recognition for
   * @param phraseToName Mapping from spoken phrase -> spell name
   * @param language Language code (e.g., "en-US")
   */
  public static void startVoiceRecognition(
      ServerPlayer player, Map<String, String> phraseToName, String language) {
    if (!isVoiceRecognitionActive(player)) {
      activeVoiceRecognition.add(player.getUUID());

      // Send config to client to start the sidecar
      PacketHandler.INSTANCE.send(
          new VoiceSidecarConfigPacket(language, phraseToName), player.connection.getConnection());

      System.out.println(
          "Started voice recognition for player: "
              + player.getName().getString()
              + " with "
              + (phraseToName != null ? phraseToName.size() : 0)
              + " mappings in language: "
              + language);
    }
  }

  /**
   * Stops voice recognition for a player.
   *
   * @param player The ServerPlayer to stop voice recognition for
   */
  public static void stopVoiceRecognition(ServerPlayer player) {
    if (isVoiceRecognitionActive(player)) {
      activeVoiceRecognition.remove(player.getUUID());
      // TODO: Send stop packet to client if needed
      System.out.println("Stopped voice recognition for player: " + player.getName().getString());
    }
  }

  /**
   * Updates the configuration for an active voice recognition session.
   *
   * @param player The ServerPlayer to update configuration for
   * @param language New language code
   * @param phraseToName New mapping from spoken phrase -> spell name
   */
  public static void updateConfiguration(
      ServerPlayer player, String language, Map<String, String> phraseToName) {
    if (isVoiceRecognitionActive(player)) {
      // Send updated config to client
      PacketHandler.INSTANCE.send(
          new VoiceSidecarConfigPacket(language, phraseToName), player.connection.getConnection());

      System.out.println(
          "Updated voice recognition config for player: "
              + player.getName().getString()
              + " with "
              + (phraseToName != null ? phraseToName.size() : 0)
              + " mappings in language: "
              + language);
    }
  }

  /**
   * Checks if voice recognition is currently active for a player.
   *
   * @param player The ServerPlayer to check
   * @return true if voice recognition is active, false otherwise
   */
  public static boolean isVoiceRecognitionActive(ServerPlayer player) {
    return activeVoiceRecognition.contains(player.getUUID());
  }

  /** Shuts down all active voice recognition instances. Should be called during server shutdown. */
  public static void shutdownAll() {
    activeVoiceRecognition.clear();
    System.out.println("Shut down all voice recognition instances");
  }
}

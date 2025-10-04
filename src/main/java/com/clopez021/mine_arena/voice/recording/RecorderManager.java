package com.clopez021.mine_arena.voice.recording;

import com.mojang.logging.LogUtils;
import de.maxhenkel.voicechat.api.VoicechatApi;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;

public class RecorderManager {
  private static final Logger LOGGER = LogUtils.getLogger();
  private static VoicechatApi voicechatApi;
  private static final Map<UUID, Recorder> activeRecorders = new ConcurrentHashMap<>();

  public static void init(VoicechatApi api) {
    voicechatApi = api;
    LOGGER.info("RecorderManager initialized with VoiceChat API");
  }

  public static VoicechatApi getApi() {
    return voicechatApi;
  }

  /**
   * Starts recording for a specific player (automatically stops after 5 seconds)
   *
   * @param playerUuid The UUID of the player to record
   * @param command The command that triggered the recording (for filename)
   * @return true if recording started successfully
   */
  public static boolean startRecording(UUID playerUuid, String command) {
    if (activeRecorders.containsKey(playerUuid)) {
      LOGGER.warn("Player {} is already being recorded", playerUuid);
      return false;
    }

    try {
      Recorder recorder = new Recorder(playerUuid, command);
      activeRecorders.put(playerUuid, recorder);
      LOGGER.info("Started 5-second recording for player {} with command: {}", playerUuid, command);

      // Schedule automatic stop after 5 seconds
      recorder.scheduleAutoStop();

      return true;
    } catch (Exception e) {
      LOGGER.error("Failed to start recording for player {}: {}", playerUuid, e.getMessage());
      return false;
    }
  }

  /**
   * Stops recording for a specific player
   *
   * @param playerUuid The UUID of the player to stop recording
   * @return The filename of the saved recording, or null if failed
   */
  public static String stopRecording(UUID playerUuid) {
    Recorder recorder = activeRecorders.remove(playerUuid);
    if (recorder == null) {
      LOGGER.warn("No active recording found for player {}", playerUuid);
      return null;
    }

    try {
      String filename = recorder.stopRecording();
      LOGGER.info("Stopped recording for player {}, saved as: {}", playerUuid, filename);
      return filename;
    } catch (Exception e) {
      LOGGER.error("Failed to stop recording for player {}: {}", playerUuid, e.getMessage());
      return null;
    }
  }

  /**
   * Checks if a player is currently being recorded
   *
   * @param playerUuid The UUID of the player to check
   * @return true if the player is being recorded
   */
  public static boolean isRecording(UUID playerUuid) {
    return activeRecorders.containsKey(playerUuid);
  }

  /**
   * Gets the recorder for a specific player
   *
   * @param playerUuid The UUID of the player
   * @return The recorder instance, or null if not found
   */
  public static Recorder getRecorder(UUID playerUuid) {
    return activeRecorders.get(playerUuid);
  }

  /**
   * Removes a recorder from active recorders (used by auto-stop)
   *
   * @param playerUuid The UUID of the player
   */
  public static void removeRecorder(UUID playerUuid) {
    activeRecorders.remove(playerUuid);
  }

  /** Stops all active recordings (useful for server shutdown) */
  public static void stopAllRecordings() {
    for (Map.Entry<UUID, Recorder> entry : activeRecorders.entrySet()) {
      try {
        entry.getValue().stopRecording();
        LOGGER.info("Emergency stopped recording for player {}", entry.getKey());
      } catch (Exception e) {
        LOGGER.error(
            "Failed to emergency stop recording for player {}: {}", entry.getKey(), e.getMessage());
      }
    }
    activeRecorders.clear();

    // Shutdown the scheduler
    Recorder.shutdown();
  }
}

package com.clopez021.mine_arena.player;

import com.clopez021.mine_arena.MineArena;
import com.clopez021.mine_arena.spell.config.PlayerSpellConfig;
import com.clopez021.mine_arena.voice.recognition.SpeechCommand;
import com.clopez021.mine_arena.voice.recognition.SpeechRecognitionManager;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Simple router that manages Player objects by UUID. Clean, minimal manager that delegates to
 * Player model objects. Uses singleton pattern for proper state management.
 */
@Mod.EventBusSubscriber(modid = "mine_arena")
public class PlayerManager {

  private static PlayerManager instance = new PlayerManager();

  // Simple dictionary: UUID -> Player
  private final Map<UUID, Player> players = new ConcurrentHashMap<>();

  private PlayerManager() {
    // Private constructor for singleton
  }

  /**
   * Gets the singleton instance of PlayerManager.
   *
   * @return The PlayerManager instance
   */
  public static PlayerManager getInstance() {
    if (instance == null) {
      synchronized (PlayerManager.class) {
        if (instance == null) {
          instance = new PlayerManager();
        }
      }
    }
    return instance;
  }

  /**
   * Gets a Player object for the given ServerPlayer. Returns null if no player exists.
   *
   * @param serverPlayer The ServerPlayer
   * @return Player object or null if not found
   */
  public Player getPlayer(ServerPlayer serverPlayer) {
    return players.get(serverPlayer.getUUID());
  }

  /**
   * Creates a new Player object for the given ServerPlayer.
   *
   * @param serverPlayer The ServerPlayer
   * @return The created Player object
   */
  public Player createPlayer(ServerPlayer serverPlayer) {
    Player player = new Player(serverPlayer);
    players.put(serverPlayer.getUUID(), player);
    return player;
  }

  /**
   * Bulk add spells to a player (merge by spell name).
   *
   * @param serverPlayer The ServerPlayer to add spells for
   * @param spells Collection of PlayerSpell objects
   */
  public void addSpells(ServerPlayer serverPlayer, java.util.Collection<PlayerSpellConfig> spells) {
    System.out.println("addSpells: " + spells);
    Player player = getPlayer(serverPlayer);
    if (player != null) {
      player.addSpells(spells);
    }
  }

  /**
   * Sets the language for a player.
   *
   * @param serverPlayer The ServerPlayer to set language for
   * @param language Language code (e.g., "en-US")
   */
  public void setLanguage(ServerPlayer serverPlayer, String language) {
    Player player = getPlayer(serverPlayer);
    if (player != null) {
      player.setLanguage(language);
    }
  }

  /** Adds a single spell by PlayerSpell object. */
  public void addSpell(ServerPlayer serverPlayer, PlayerSpellConfig spell) {
    Player player = getPlayer(serverPlayer);
    if (player != null) {
      player.addSpell(spell);
    }
  }

  /**
   * Removes a spell from a player's recognition list by spell name.
   *
   * @param serverPlayer The ServerPlayer to remove spell for
   * @param name The spell name to remove
   */
  public void removeSpell(ServerPlayer serverPlayer, String name) {
    Player player = getPlayer(serverPlayer);
    if (player != null) {
      player.removeSpell(name);
    }
  }

  /**
   * Starts voice recognition for a player.
   *
   * @param serverPlayer The ServerPlayer to start voice recognition for
   */
  public void startVoiceRecognition(ServerPlayer serverPlayer) {
    Player player = getPlayer(serverPlayer);
    if (player != null) {
      player.startVoiceRecognition();
    }
  }

  /**
   * Stops voice recognition for a player.
   *
   * @param serverPlayer The ServerPlayer to stop voice recognition for
   */
  public void stopVoiceRecognition(ServerPlayer serverPlayer) {
    Player player = getPlayer(serverPlayer);
    if (player != null) {
      player.stopVoiceRecognition();
    }
  }

  /**
   * Main entry point for handling recognized speech commands.
   *
   * @param serverPlayer The ServerPlayer who spoke the command
   * @param command The recognized speech command
   */
  public void handleSpeechCommand(ServerPlayer serverPlayer, SpeechCommand command) {
    Player player = getPlayer(serverPlayer);
    if (player == null) return;

    String spellName = command.getSpellName();

    // Output to console for debugging
    System.out.printf(
        "[Voice] Player %s cast spell: %s%n", serverPlayer.getName().getString(), spellName);

    // Output to Minecraft chat
    String chatMessage = String.format("🎤 Spell Cast: %s", spellName);
    serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal(chatMessage));

    // Spawn the player's saved spell entity using config
    player.spawnSpell(spellName);
  }

  /** Event handler for player login. */
  @SubscribeEvent
  public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
    if (event.getEntity() instanceof ServerPlayer serverPlayer) {
      PlayerManager manager = getInstance();

      // Create player object if it doesn't exist
      Player player = manager.getPlayer(serverPlayer);
      if (player == null) {
        player = manager.createPlayer(serverPlayer);
      }

      // Merge in default spells for new/returning players as-is (owner/look bound at cast time)
      manager.addSpells(serverPlayer, MineArena.getDefaultSpells());

      // Auto-start voice recognition on login
      player.startVoiceRecognition();
    }
  }

  /** Event handler for player logout - clean up voice recognition and player object. */
  @SubscribeEvent
  public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
    if (event.getEntity() instanceof ServerPlayer serverPlayer) {
      PlayerManager manager = getInstance();
      manager.stopVoiceRecognition(serverPlayer);
      // Remove player object from memory (data is already persisted to SavedData)
      manager.players.remove(serverPlayer.getUUID());
    }
  }

  /**
   * Event handler for player respawn/clone after death. Updates the ServerPlayer reference in the
   * Player object to ensure spell casting and voice recognition work correctly after respawn.
   */
  @SubscribeEvent
  public static void onPlayerClone(PlayerEvent.Clone event) {
    // Only handle death respawns, not dimension changes
    if (!event.isWasDeath()) {
      return;
    }

    if (event.getEntity() instanceof ServerPlayer newServerPlayer) {
      PlayerManager manager = getInstance();
      Player player = manager.getPlayer(newServerPlayer);

      // Update the ServerPlayer reference to the new respawned player
      if (player != null) {
        player.updateServerPlayer(newServerPlayer);
      }
    }
  }

  /**
   * Shuts down all player management and speech recognition. Should be called during server
   * shutdown.
   */
  public void shutdown() {
    // Stop voice recognition for all players
    players.values().forEach(Player::stopVoiceRecognition);
    SpeechRecognitionManager.shutdownAll();
    players.clear();
  }

  /** Static convenience method for shutdown to maintain compatibility. */
  public static void shutdownAll() {
    if (instance != null) {
      instance.shutdown();
    }
  }
}

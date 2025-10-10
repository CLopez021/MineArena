package com.clopez021.mine_arena.player;

import com.clopez021.mine_arena.core.entity.ModEntities;
import com.clopez021.mine_arena.core.entity.SpellEntity;
import com.clopez021.mine_arena.model3d.ObjModel;
import com.clopez021.mine_arena.network.PacketHandler;
import com.clopez021.mine_arena.network.SpellCompletePacket;
import com.clopez021.mine_arena.spell.config.PlayerSpellConfig;
import com.clopez021.mine_arena.spell.config.SpellEntityConfig;
import com.clopez021.mine_arena.voice.recognition.SpeechRecognitionManager;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

/**
 * Player model that encapsulates player-specific data and behavior. Handles its own speech
 * recognition and data management.
 */
public class Player {
  private final UUID uuid;
  // name -> PlayerSpellConfig (runtime lookup by name)
  private final Map<String, PlayerSpellConfig> spells;
  // Cooldown tracking: spellName -> last cast time in milliseconds
  private final Map<String, Long> lastCastTimes;
  private String language;
  private ServerPlayer serverPlayer; // Reference to update speech recognition

  // Default values
  private static final Map<String, PlayerSpellConfig> DEFAULT_SPELLS = Map.of();
  private static final String DEFAULT_LANGUAGE = "en-US";

  public Player(ServerPlayer serverPlayer) {
    this.uuid = serverPlayer.getUUID();
    this.spells = new HashMap<>(DEFAULT_SPELLS);
    this.lastCastTimes = new HashMap<>();
    this.language = DEFAULT_LANGUAGE;
    this.serverPlayer = serverPlayer;

    // Load data from per-player persistent NBT
    loadData();
  }

  // Getters
  public UUID getUuid() {
    return uuid;
  }

  public Collection<PlayerSpellConfig> getSpells() {
    return new java.util.ArrayList<>(spells.values());
  }

  /**
   * Updates the ServerPlayer reference. This is needed when a player respawns after death, as
   * Minecraft creates a new ServerPlayer instance.
   *
   * @param newServerPlayer The new ServerPlayer instance
   */
  public void updateServerPlayer(ServerPlayer newServerPlayer) {
    if (newServerPlayer.getUUID().equals(this.uuid)) {
      this.serverPlayer = newServerPlayer;
      updateSpeechRecognition();
    }
  }

  // Bulk-add with auto-save and speech recognition updates
  public void addSpells(Collection<PlayerSpellConfig> spells) {
    System.out.println("addSpells: " + spells);
    boolean changed = false;
    for (PlayerSpellConfig ps : spells) {
      PlayerSpellConfig prev = this.spells.put(ps.name(), ps);
      if (prev == null || !prev.equals(ps)) changed = true;
    }
    if (changed) {
      saveData();
      updateSpeechRecognition();
    }
  }

  public void setLanguage(String language) {
    this.language = language != null ? language : DEFAULT_LANGUAGE;
    saveData();
    updateSpeechRecognition();
  }

  // Spell management with auto-save and speech recognition updates
  public void addSpell(PlayerSpellConfig spell) {
    String key = spell.name();
    spells.put(key, spell);
    saveData();
    updateSpeechRecognition();
  }

  public boolean removeSpell(String name) {
    boolean removed = spells.remove(name) != null;
    if (removed) {
      saveData();
      updateSpeechRecognition();
    }
    return removed;
  }

  // Data persistence
  /**
   * Loads this player's data from the player's persistent NBT data. Stored under root key
   * "mine_arena" with keys: - Spells: ListTag of Compound { name, phrase, entityData } - Language:
   * String
   */
  private void loadData() {
    if (serverPlayer != null) {
      try {
        CompoundTag root = serverPlayer.getPersistentData().getCompound("mine_arena");

        // Load spells
        this.spells.clear();
        if (root.contains("Spells", Tag.TAG_LIST)) {
          ListTag list = root.getList("Spells", Tag.TAG_COMPOUND);
          for (Tag t : list) {
            if (t instanceof CompoundTag ct) {
              try {
                PlayerSpellConfig ps = PlayerSpellConfig.fromNBT(ct);
                if (!ps.name().isBlank() && !ps.phrase().isBlank()) {
                  this.spells.put(ps.name(), ps);
                }
              } catch (IllegalArgumentException ignored) {
              }
            }
          }
        }

        // Load language
        if (root.contains("Language", Tag.TAG_STRING)) {
          this.language = root.getString("Language");
        }
      } catch (Exception e) {
        System.err.println("Failed to load player data: " + e.getMessage());
        // Keep defaults
      }
    }
  }

  /** Persists this player's data to the player's persistent NBT data. */
  public void saveData() {
    if (serverPlayer != null) {
      try {
        CompoundTag persistent = serverPlayer.getPersistentData();
        CompoundTag root = persistent.getCompound("mine_arena");

        // Save spells
        ListTag list = new ListTag();
        for (PlayerSpellConfig ps : spells.values()) {
          list.add(ps.toNBT());
        }
        root.put("Spells", list);

        // Save language
        root.putString("Language", language);

        // Write back to player's persistent data
        persistent.put("mine_arena", root);
      } catch (Exception e) {
        System.err.println("Failed to save player data: " + e.getMessage());
      }
    }
  }

  // Speech recognition management
  public void startVoiceRecognition() {
    if (serverPlayer != null) {
      Map<String, String> phraseToName = new HashMap<>();
      for (PlayerSpellConfig ps : spells.values()) phraseToName.put(ps.phrase(), ps.name());
      SpeechRecognitionManager.startVoiceRecognition(serverPlayer, phraseToName, language);
    }
  }

  public void stopVoiceRecognition() {
    if (serverPlayer != null) {
      SpeechRecognitionManager.stopVoiceRecognition(serverPlayer);
    }
  }

  private void updateSpeechRecognition() {
    if (serverPlayer != null && SpeechRecognitionManager.isVoiceRecognitionActive(serverPlayer)) {
      Map<String, String> phraseToName = new HashMap<>();
      for (PlayerSpellConfig ps : spells.values()) phraseToName.put(ps.phrase(), ps.name());
      SpeechRecognitionManager.updateConfiguration(serverPlayer, language, phraseToName);
    }
  }

  /** Spawn the spell entity for the given spell name using this player's saved config. */
  public void spawnSpell(String spellName) {
    if (serverPlayer == null || serverPlayer.server == null) return;
    if (spellName == null || spellName.isBlank()) return;

    PlayerSpellConfig ps = spells.get(spellName);
    if (ps == null) {
      return;
    }

    // Check cooldown
    long currentTime = System.currentTimeMillis();
    Long lastCastTime = lastCastTimes.get(spellName);
    if (lastCastTime != null) {
      long timeSinceLastCast = currentTime - lastCastTime;
      if (timeSinceLastCast < ps.cooldownMillis()) {
        // Spell is still on cooldown - ignore this cast
        return;
      }
    }

    // Record cast time
    lastCastTimes.put(spellName, currentTime);

    SpellEntityConfig base = ps.config();
    // Rotate blocks to match the player's yaw/pitch at cast time.
    var rotatedBlocks =
        ObjModel.rotateBlocks3D(base.getBlocks(), serverPlayer.getYRot(), serverPlayer.getXRot());
    SpellEntityConfig cfg =
        new SpellEntityConfig(
            rotatedBlocks,
            base.getMicroScaleRaw(), // Use raw normalized value to preserve scale
            base.getEffectBehavior(),
            base.getShouldMove(),
            base.getMovementSpeed());

    serverPlayer.server.execute(
        () -> {
          var level = serverPlayer.level();
          var entityType = ModEntities.SPELL_ENTITY.get();
          SpellEntity e = entityType.create(level);
          if (e != null) {
            Vec3 pos = serverPlayer.position();
            e.setYRot(serverPlayer.getYRot());
            e.setXRot(serverPlayer.getXRot());
            e.setOwnerPlayerId(serverPlayer.getUUID());

            // Initialize first to calculate bounds
            e.initializeServer(cfg);

            // Now adjust position so bottom of spell is slightly above player's feet
            float minY = e.minCorner.y * cfg.getMicroScale();
            float padding = 0.2f;
            double adjustedY = pos.y - minY + padding;
            e.setPos(pos.x, adjustedY, pos.z);

            level.addFreshEntity(e);
          }

          if (serverPlayer.connection != null) {
            PacketHandler.INSTANCE.send(
                new SpellCompletePacket(), PacketDistributor.PLAYER.with(serverPlayer));
          }
        });
  }
}

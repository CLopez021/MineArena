package com.clopez021.mine_arena.spell.config;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Immutable configuration for a player's spell with name, phrase, entity config, and cooldown. */
public class PlayerSpellConfig extends BaseConfig {
  private final String name;
  private final String phrase;
  private final SpellEntityConfig config;
  private final float cooldownSeconds; // Cooldown in seconds

  public PlayerSpellConfig(
      String name, String phrase, SpellEntityConfig config, float cooldownSeconds) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must be non-empty");
    }
    if (phrase == null || phrase.isBlank()) {
      throw new IllegalArgumentException("phrase must be non-empty");
    }
    if (config == null) {
      throw new IllegalArgumentException("config must be non-null");
    }
    if (cooldownSeconds < 0) {
      throw new IllegalArgumentException("cooldown must be non-negative");
    }
    this.name = name;
    this.phrase = phrase;
    this.config = config;
    this.cooldownSeconds = cooldownSeconds;
  }

  public String name() {
    return name;
  }

  public String phrase() {
    return phrase;
  }

  public SpellEntityConfig config() {
    return config;
  }

  public float cooldownSeconds() {
    return cooldownSeconds;
  }

  public long cooldownMillis() {
    return (long) (cooldownSeconds * 1000);
  }

  @Override
  public CompoundTag toNBT() {
    CompoundTag tag = new CompoundTag();
    tag.putString("name", name);
    tag.putString("phrase", phrase);
    tag.put("entityData", config.toNBT());
    tag.putFloat("cooldownSeconds", cooldownSeconds);
    return tag;
  }

  public static PlayerSpellConfig fromNBT(CompoundTag tag) {
    String name = tag.getString("name");
    String phrase = tag.getString("phrase");
    SpellEntityConfig cfg =
        tag.contains("entityData", Tag.TAG_COMPOUND)
            ? SpellEntityConfig.fromNBT(tag.getCompound("entityData"))
            : SpellEntityConfig.empty();
    float cooldownSeconds =
        tag.contains("cooldownSeconds", Tag.TAG_FLOAT) ? tag.getFloat("cooldownSeconds") : 0.0f;
    return new PlayerSpellConfig(name, phrase, cfg, cooldownSeconds);
  }
}

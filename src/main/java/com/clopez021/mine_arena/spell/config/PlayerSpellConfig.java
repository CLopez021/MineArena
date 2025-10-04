package com.clopez021.mine_arena.spell.config;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Immutable configuration for a player's spell with name, phrase, and entity config. */
public class PlayerSpellConfig extends BaseConfig {
  private final String name;
  private final String phrase;
  private final SpellEntityConfig config;

  public PlayerSpellConfig(String name, String phrase, SpellEntityConfig config) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must be non-empty");
    }
    if (phrase == null || phrase.isBlank()) {
      throw new IllegalArgumentException("phrase must be non-empty");
    }
    if (config == null) {
      throw new IllegalArgumentException("config must be non-null");
    }
    this.name = name;
    this.phrase = phrase;
    this.config = config;
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

  @Override
  public CompoundTag toNBT() {
    CompoundTag tag = new CompoundTag();
    tag.putString("name", name);
    tag.putString("phrase", phrase);
    tag.put("entityData", config.toNBT());
    return tag;
  }

  public static PlayerSpellConfig fromNBT(CompoundTag tag) {
    String name = tag.getString("name");
    String phrase = tag.getString("phrase");
    SpellEntityConfig cfg =
        tag.contains("entityData", Tag.TAG_COMPOUND)
            ? SpellEntityConfig.fromNBT(tag.getCompound("entityData"))
            : SpellEntityConfig.empty();
    return new PlayerSpellConfig(name, phrase, cfg);
  }
}

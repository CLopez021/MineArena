package com.clopez021.mine_arena.spell;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** PlayerSpellConfig now embeds SpellEntityConfig (no file indirection). */
public record PlayerSpellConfig(String name, String phrase, SpellEntityConfig config) {

  public PlayerSpellConfig {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must be non-empty");
    }
    if (phrase == null || phrase.isBlank()) {
      throw new IllegalArgumentException("phrase must be non-empty");
    }
    if (config == null) {
      throw new IllegalArgumentException("config must be non-null");
    }
  }

  /** Serialize to NBT using SpellEntityConfig helpers for the nested data. */
  public CompoundTag toNBT() {
    CompoundTag tag = new CompoundTag();
    tag.putString("name", name);
    tag.putString("phrase", phrase);
    tag.put("entityData", config.toNBT());
    return tag;
  }

  /** Deserialize from NBT using SpellEntityConfig helpers for the nested data. */
  public static PlayerSpellConfig fromNBT(CompoundTag tag) {
    String name = tag.getString("name");
    String phrase = tag.getString("phrase");
    SpellEntityConfig cfg;
    if (tag.contains("entityData", Tag.TAG_COMPOUND)) {
      cfg = SpellEntityConfig.fromNBT(tag.getCompound("entityData"));
    } else {
      cfg = SpellEntityConfig.empty();
    }
    return new PlayerSpellConfig(name, phrase, cfg);
  }
}

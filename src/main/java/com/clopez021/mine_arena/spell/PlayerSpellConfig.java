package com.clopez021.mine_arena.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * PlayerSpellConfig embeds SpellEntityData directly (no file indirection).
 */
public record PlayerSpellConfig(String name, String phrase, SpellEntityData data) {
    public static final Codec<PlayerSpellConfig> CODEC = RecordCodecBuilder.create(i ->
        i.group(
            Codec.STRING.fieldOf("name").forGetter(PlayerSpellConfig::name),
            Codec.STRING.fieldOf("phrase").forGetter(PlayerSpellConfig::phrase),
            SpellEntityData.CODEC.fieldOf("data").forGetter(PlayerSpellConfig::data)
        ).apply(i, PlayerSpellConfig::new)
    );

    public PlayerSpellConfig {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must be non-empty");
        }
        if (phrase == null || phrase.isBlank()) {
            throw new IllegalArgumentException("phrase must be non-empty");
        }
        if (data == null) {
            throw new IllegalArgumentException("data must be non-null");
        }
    }

    /**
     * Serialize to NBT using SpellEntityInitData helpers for the nested data.
     */
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", name);
        tag.putString("phrase", phrase);
        SpellEntityConfig spellEntity = new SpellEntityConfig(data.blocks(), data.microScale());
        tag.put("entityData", spellEntity.toNBT());
        return tag;
    }

    /**
     * Deserialize from NBT using SpellEntityInitData helpers for the nested data.
     */
    public static PlayerSpellConfig fromNBT(CompoundTag tag) {
        String name = tag.getString("name");
        String phrase = tag.getString("phrase");
        SpellEntityData data;
        if (tag.contains("entityData", Tag.TAG_COMPOUND)) {
            SpellEntityConfig init = SpellEntityConfig.fromNBT(tag.getCompound("entityData"));
            data = new SpellEntityData(init.blocks(), init.microScale());
        } else {
            data = SpellEntityData.empty();
        }
        return new PlayerSpellConfig(name, phrase, data);
    }
}

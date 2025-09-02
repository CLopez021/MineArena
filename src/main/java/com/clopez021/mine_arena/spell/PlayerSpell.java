package com.clopez021.mine_arena.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * PlayerSpell now embeds SpellEntityData directly (no file indirection).
 */
public record PlayerSpell(String name, String phrase, SpellEntityData data) {
    public static final Codec<PlayerSpell> CODEC = RecordCodecBuilder.create(i ->
        i.group(
            Codec.STRING.fieldOf("name").forGetter(PlayerSpell::name),
            Codec.STRING.fieldOf("phrase").forGetter(PlayerSpell::phrase),
            SpellEntityData.CODEC.fieldOf("data").forGetter(PlayerSpell::data)
        ).apply(i, PlayerSpell::new)
    );

    public PlayerSpell {
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
}

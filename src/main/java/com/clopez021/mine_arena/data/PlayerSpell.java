package com.clopez021.mine_arena.data;

/**
 * Simple immutable mapping of a spell phrase to its file path.
 */
public record PlayerSpell(String spell, String file) {
    public PlayerSpell {
        if (spell == null || spell.isBlank()) {
            throw new IllegalArgumentException("spell must be non-empty");
        }
        if (file == null || file.isBlank()) {
            throw new IllegalArgumentException("file must be non-empty");
        }
    }
}

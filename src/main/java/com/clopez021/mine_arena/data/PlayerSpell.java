package com.clopez021.mine_arena.data;

/**
 * PlayerSpell is the value stored for a player's spell entry.
 *
 * The record includes:
 * - name: display name of the spell (human-friendly label)
 * - phrase: the exact spoken phrase that triggers the spell (map key)
 * - entityDataFile: absolute or world-relative path to the spell's entity init JSON/model
 *
 * Note on null checks: Java does not enforce non-null at runtime for reference types
 * even in records. These checks ensure invalid state never enters the save data or
 * runtime collections.
 */
public record PlayerSpell(String name, String phrase, String entityDataFile) {
    public PlayerSpell {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must be non-empty");
        }
        if (phrase == null || phrase.isBlank()) {
            throw new IllegalArgumentException("phrase must be non-empty");
        }
        if (entityDataFile == null || entityDataFile.isBlank()) {
            throw new IllegalArgumentException("entityDataFile must be non-empty");
        }
        // Enforce relative path (avoid absolute paths)
        String f = entityDataFile.trim();
        if (f.startsWith("/") || f.matches("^[A-Za-z]:.*")) {
            throw new IllegalArgumentException("entityDataFile must be a relative path");
        }
        if (f.contains("..")) {
            throw new IllegalArgumentException("entityDataFile must not contain path traversal '..'");
        }
    }

    /**
     * Returns the stored file path as a relative Path object.
     */
    public java.nio.file.Path asRelativePath() {
        return java.nio.file.Path.of(entityDataFile);
    }
}

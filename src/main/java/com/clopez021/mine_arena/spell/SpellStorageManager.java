package com.clopez021.mine_arena.spell;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Manages saving/loading spell init data JSON under world/data/mine_arena.
 * Callers store and pass relative paths (no absolute or traversal) returned by saveSpell.
 */
public class SpellStorageManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path baseDirectory;

    public SpellStorageManager(MinecraftServer server) {
        Path root = server.getWorldPath(LevelResource.ROOT);
        this.baseDirectory = root.resolve("data").resolve("mine_arena");
        try {
            Files.createDirectories(baseDirectory);
        } catch (IOException e) {
            LOGGER.error("Failed to create base directory: {}", baseDirectory, e);
        }
    }

    private static String sanitizeFilename(String name) {
        return name.trim()
            .replaceAll("[/\\\\:*?\"<>|]", "_")
            .replaceAll("\\.\\.+", "_")
            .replaceAll("^\\.", "_")
            .replaceAll("\\.$", "_");
    }

    private Path resolveRelative(String relativePath) {
        return baseDirectory.resolve(relativePath);
    }

    /**
     * Saves model under playerId namespace and returns relative path to store on the PlayerSpell.
     * Relative path format: "{playerId}/{sanitizedSpellName}.json".
     */
    public String saveSpell(UUID playerId, String spellName, SpellEntityInitData initData) {
        if (playerId == null || spellName == null || spellName.trim().isEmpty()) {
            LOGGER.warn("Cannot save spell: invalid playerId or spellName");
            return null;
        }

        String relative = playerId.toString() + "/" + sanitizeFilename(spellName) + ".json";
        Path file = resolveRelative(relative);
        try {
            Files.createDirectories(file.getParent());
            JsonObject spellJson = new JsonObject();
            spellJson.add("model", initData.toJson());
            Files.writeString(file, GSON.toJson(spellJson));
            LOGGER.info("Saved spell '{}' for {} at {}", spellName, playerId, file);
            return relative;
        } catch (IOException e) {
            LOGGER.error("Failed to save spell '{}' for {} to {}", spellName, playerId, file, e);
            return null;
        }
    }

    /**
     * Loads spell init data using a previously stored relative path.
     */
    public SpellEntityInitData loadSpell(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return null;
        Path file = resolveRelative(relativePath);
        if (!Files.exists(file)) return null;
        try {
            String jsonString = Files.readString(file);
            JsonObject spellJson = JsonParser.parseString(jsonString).getAsJsonObject();
            if (!spellJson.has("model")) return null;
            return SpellEntityInitData.fromJson(spellJson.getAsJsonObject("model"));
        } catch (Exception e) {
            LOGGER.error("Failed to load spell at {}", file, e);
            return null;
        }
    }

    public boolean spellExists(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return false;
        return Files.exists(resolveRelative(relativePath));
    }

    public boolean deleteSpell(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return false;
        Path file = resolveRelative(relativePath);
        try {
            if (Files.exists(file)) {
                Files.delete(file);
            }
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to delete spell at {}", file, e);
            return false;
        }
    }

    public Path getBaseDirectory() { return baseDirectory; }
}

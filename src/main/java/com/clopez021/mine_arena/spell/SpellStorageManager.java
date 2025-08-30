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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Server-side spell storage manager that handles saving and loading spell data to/from JSON files
 * in the world directory, organized by player ID. This ensures spells persist with the world save data
 * and each player has their own spell collection.
 */
public class SpellStorageManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path baseDirectory;
    
    public SpellStorageManager(MinecraftServer server) {
        // Get the world root path and create our base directory structure
        Path root = server.getWorldPath(LevelResource.ROOT);
        this.baseDirectory = root.resolve("data").resolve("mine_arena");
        
        // Ensure the base directory exists
        try {
            Files.createDirectories(baseDirectory);
        } catch (IOException e) {
            LOGGER.error("Failed to create base directory: {}", baseDirectory, e);
        }
    }
    
    /**
     * Gets the player-specific spells directory for a given player ID.
     * 
     * @param playerId The player's UUID
     * @return The path to the player's spells directory
     */
    private Path getPlayerSpellsDirectory(UUID playerId) {
        return baseDirectory.resolve(playerId.toString());
    }
    
    /**
     * Saves a spell to disk for a specific player.
     * The file will be saved as mine_arena/{playerId}/{spellName}.json
     *
     * @param playerId The player's UUID
     * @param spellName The name of the spell (used as filename)
     * @param initData The spell initialization data to save
     * @return The Path to the saved file if successful, null otherwise
     */
    public Path saveSpell(UUID playerId, String spellName, SpellEntityInitData initData) {
        if (spellName == null || spellName.trim().isEmpty()) {
            LOGGER.warn("Cannot save spell with null or empty name for player {}", playerId);
            return null;
        }
        
        // Get player's spells directory and ensure it exists
        Path playerDir = getPlayerSpellsDirectory(playerId);
        try {
            Files.createDirectories(playerDir);
        } catch (IOException e) {
            LOGGER.error("Failed to create player spells directory: {}", playerDir, e);
            return null;
        }
        
        // Sanitize filename to prevent path traversal
        String sanitizedName = sanitizeFilename(spellName);
        Path spellFile = playerDir.resolve(sanitizedName + ".json");
        
        try {
            // Create the main JSON structure with the model data
            JsonObject spellJson = new JsonObject();
            spellJson.add("model", initData.toJson());
            
            // Write to file
            String jsonString = GSON.toJson(spellJson);
            Files.writeString(spellFile, jsonString);
            
            LOGGER.info("Successfully saved spell '{}' for player {} to {}", spellName, playerId, spellFile);
            return spellFile;
            
        } catch (IOException e) {
            LOGGER.error("Failed to save spell '{}' for player {} to {}", spellName, playerId, spellFile, e);
            return null;
        }
    }
    
    /**
     * Loads a spell from disk for a specific player.
     * 
     * @param playerId The player's UUID
     * @param spellName The name of the spell to load
     * @return The spell initialization data, or null if not found or failed to load
     */
    public SpellEntityInitData loadSpell(UUID playerId, String spellName) {
        if (playerId == null) {
            LOGGER.warn("Cannot load spell with null player ID");
            return null;
        }
        if (spellName == null || spellName.trim().isEmpty()) {
            LOGGER.warn("Cannot load spell with null or empty name for player {}", playerId);
            return null;
        }
        
        String sanitizedName = sanitizeFilename(spellName);
        Path spellFile = getPlayerSpellsDirectory(playerId).resolve(sanitizedName + ".json");
        
        if (!Files.exists(spellFile)) {
            LOGGER.debug("Spell file does not exist for player {}: {}", playerId, spellFile);
            return null;
        }
        
        try {
            String jsonString = Files.readString(spellFile);
            JsonObject spellJson = JsonParser.parseString(jsonString).getAsJsonObject();
            
            if (!spellJson.has("model")) {
                LOGGER.error("Spell file '{}' for player {} is missing 'model' property", spellFile, playerId);
                return null;
            }
            
            JsonObject modelJson = spellJson.getAsJsonObject("model");
            SpellEntityInitData initData = SpellEntityInitData.fromJson(modelJson);
            
            LOGGER.info("Successfully loaded spell '{}' for player {} from {}", spellName, playerId, spellFile);
            return initData;
            
        } catch (IOException e) {
            LOGGER.error("Failed to read spell file '{}' for player {}", spellFile, playerId, e);
            return null;
        } catch (Exception e) {
            LOGGER.error("Failed to parse spell file '{}' for player {}", spellFile, playerId, e);
            return null;
        }
    }
    
    // listPlayerSpells removed per simplification requirement
    
    /**
     * Checks if a spell exists on disk.
     * 
     * @param playerId The player's UUID
     * @param spellName The name of the spell to check
     * @return true if the spell file exists, false otherwise
     */
    public boolean spellExists(UUID playerId, String spellName) {
        if (playerId == null || spellName == null || spellName.trim().isEmpty()) {
            return false;
        }
        
        String sanitizedName = sanitizeFilename(spellName);
        Path spellFile = getPlayerSpellsDirectory(playerId).resolve(sanitizedName + ".json");
        return Files.exists(spellFile);
    }
    
    /**
     * Deletes a spell for a specific player.
     * 
     * @param playerId The player's UUID
     * @param spellName The name of the spell to delete
     * @return true if deleted successfully, false otherwise
     */
    public boolean deleteSpell(UUID playerId, String spellName) {
        if (playerId == null) {
            LOGGER.warn("Cannot delete spell with null player ID");
            return false;
        }
        if (spellName == null || spellName.trim().isEmpty()) {
            LOGGER.warn("Cannot delete spell with null or empty name for player {}", playerId);
            return false;
        }
        
        String sanitizedName = sanitizeFilename(spellName);
        Path spellFile = getPlayerSpellsDirectory(playerId).resolve(sanitizedName + ".json");
        
        if (!Files.exists(spellFile)) {
            LOGGER.warn("Cannot delete spell '{}' for player {} - file does not exist: {}", spellName, playerId, spellFile);
            return false;
        }
        
        try {
            Files.delete(spellFile);
            LOGGER.info("Successfully deleted spell '{}' for player {} from {}", spellName, playerId, spellFile);
            return true;
            
        } catch (IOException e) {
            LOGGER.error("Failed to delete spell '{}' for player {} from {}", spellName, playerId, spellFile, e);
            return false;
        }
    }
    
    // deleteAllPlayerSpells removed per simplification requirement
    
    /**
     * Sanitizes a filename to prevent path traversal and ensure filesystem compatibility.
     * 
     * @param name The original filename
     * @return The sanitized filename
     */
    private String sanitizeFilename(String name) {
        // Remove any path separators and dangerous characters
        return name.trim()
            .replaceAll("[/\\\\:*?\"<>|]", "_")  // Replace dangerous chars with underscore
            .replaceAll("\\.\\.+", "_")          // Replace .. with underscore
            .replaceAll("^\\.", "_")             // Replace leading dot
            .replaceAll("\\.$", "_");            // Replace trailing dot
    }
    
    /**
     * Gets the base directory path.
     * 
     * @return The path to the base mine_arena directory
     */
    public Path getBaseDirectory() {
        return baseDirectory;
    }
    

} 

package com.clopez021.mine_arena.player;

import com.clopez021.mine_arena.data.PlayerSpellData;
import com.clopez021.mine_arena.data.PlayerSpell;
import com.clopez021.mine_arena.speech_recognition.SpeechRecognitionManager;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Player model that encapsulates player-specific data and behavior.
 * Handles its own speech recognition and data management.
 */
public class Player {
    private final UUID uuid;
    // phrase -> PlayerSpell
    private final Map<String, PlayerSpell> spells;
    private String language;
    private ServerPlayer serverPlayer; // Reference to update speech recognition
    
    // Default values
    private static final Map<String, PlayerSpell> DEFAULT_SPELLS = Map.of();
    private static final String DEFAULT_LANGUAGE = "en-US";
    
    public Player(ServerPlayer serverPlayer) {
        this.uuid = serverPlayer.getUUID();
        this.spells = new HashMap<>(DEFAULT_SPELLS);
        this.language = DEFAULT_LANGUAGE;
        this.serverPlayer = serverPlayer;
        
        // Load data from SavedData
        loadData();
    }
    
    // Getters
    public UUID getUuid() {
        return uuid;
    }
    
    // Setters with auto-save and speech recognition updates
    public void setSpells(Map<String, PlayerSpell> spells) {
        this.spells.clear();
        this.spells.putAll(spells);
        saveData();
        updateSpeechRecognition();
    }
    
    public void setLanguage(String language) {
        this.language = language != null ? language : DEFAULT_LANGUAGE;
        saveData();
        updateSpeechRecognition();
    }
    
    // Spell management with auto-save and speech recognition updates
    public void addSpell(String phrase, String entityDataFile) {
        // Default display name to phrase for convenience when not provided explicitly
        addSpell(new PlayerSpell(phrase, phrase, entityDataFile));
    }

    public void addSpell(PlayerSpell spell) {
        String key = spell.phrase();
        if (!spells.containsKey(key)) {
            spells.put(key, spell);
            saveData();
            updateSpeechRecognition();
        }
    }
    
    public boolean removeSpell(String spell) {
        boolean removed = spells.remove(spell) != null;
        if (removed) {
            saveData();
            updateSpeechRecognition();
        }
        return removed;
    }
    
    // Data persistence
    /**
     * Loads this player's data from SavedData.
     * Called during Player construction. The underlying SavedData instance
     * is resolved via computeIfAbsent on first access per world load.
     */
    private void loadData() {
        if (serverPlayer != null) {
            try {
                PlayerSpellData data = PlayerSpellData.get(serverPlayer.getServer());
                String playerIdStr = uuid.toString();
                Map<String, PlayerSpell> savedSpells = data.getSpells(playerIdStr);
                String savedLanguage = data.getLanguage(playerIdStr);
                
                this.spells.clear();
                this.spells.putAll(savedSpells);
                this.language = savedLanguage;
            } catch (Exception e) {
                System.err.println("Failed to load player data: " + e.getMessage());
                // Keep defaults
            }
        }
    }

    /**
     * Persists this player's data to SavedData and marks it dirty.
     * Minecraft calls SavedData#save during world saves if dirty.
     */
    public void saveData() {
        if (serverPlayer != null) {
            try {
                PlayerSpellData data = PlayerSpellData.get(serverPlayer.getServer());
                String playerIdStr = uuid.toString();
                data.setSpells(playerIdStr, spells);
                data.setLanguage(playerIdStr, language);
            } catch (Exception e) {
                System.err.println("Failed to save player data: " + e.getMessage());
            }
        }
    }
    
    // Speech recognition management
    public void startVoiceRecognition() {
        if (serverPlayer != null) {
            List<String> phrases = new ArrayList<>(spells.keySet());
            SpeechRecognitionManager.startVoiceRecognition(serverPlayer, phrases, language);
        }
    }
    
    public void stopVoiceRecognition() {
        if (serverPlayer != null) {
            SpeechRecognitionManager.stopVoiceRecognition(serverPlayer);
        }
    }
    
    private void updateSpeechRecognition() {
        if (serverPlayer != null && SpeechRecognitionManager.isVoiceRecognitionActive(serverPlayer)) {
            List<String> phrases = new ArrayList<>(spells.keySet());
            SpeechRecognitionManager.updateConfiguration(serverPlayer, language, phrases);
        }
    }
}

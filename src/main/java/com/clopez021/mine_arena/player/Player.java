package com.clopez021.mine_arena.player;

import com.clopez021.mine_arena.data.PlayerSpellData;
import com.clopez021.mine_arena.speech_recognition.SpeechRecognitionManager;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Player model that encapsulates player-specific data and behavior.
 * Handles its own speech recognition and data management.
 */
public class Player {
    private final UUID uuid;
    private final List<String> spells;
    private String language;
    private ServerPlayer serverPlayer; // Reference to update speech recognition
    
    // Default values
    private static final List<String> DEFAULT_SPELLS = List.of();
    private static final String DEFAULT_LANGUAGE = "en-US";
    
    public Player(ServerPlayer serverPlayer) {
        this.uuid = serverPlayer.getUUID();
        this.spells = new ArrayList<>(DEFAULT_SPELLS);
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
    public void setSpells(List<String> spells) {
        this.spells.clear();
        this.spells.addAll(spells);
        saveData();
        updateSpeechRecognition();
    }
    
    public void setLanguage(String language) {
        this.language = language != null ? language : DEFAULT_LANGUAGE;
        saveData();
        updateSpeechRecognition();
    }
    
    // Spell management with auto-save and speech recognition updates
    public void addSpell(String spell) {
        if (!spells.contains(spell)) {
            spells.add(spell);
            saveData();
            updateSpeechRecognition();
        }
    }
    
    public boolean removeSpell(String spell) {
        boolean removed = spells.remove(spell);
        if (removed) {
            saveData();
            updateSpeechRecognition();
        }
        return removed;
    }
    
    // Data persistence
    private void loadData() {
        if (serverPlayer != null) {
            try {
                PlayerSpellData data = PlayerSpellData.get(serverPlayer.getServer());
                String playerIdStr = uuid.toString();
                List<String> savedSpells = data.getSpells(playerIdStr);
                String savedLanguage = data.getLanguage(playerIdStr);
                
                this.spells.clear();
                this.spells.addAll(savedSpells);
                this.language = savedLanguage;
            } catch (Exception e) {
                System.err.println("Failed to load player data: " + e.getMessage());
                // Keep defaults
            }
        }
    }

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
            SpeechRecognitionManager.startVoiceRecognition(serverPlayer, spells, language);
        }
    }
    
    public void stopVoiceRecognition() {
        if (serverPlayer != null) {
            SpeechRecognitionManager.stopVoiceRecognition(serverPlayer);
        }
    }
    
    private void updateSpeechRecognition() {
        if (serverPlayer != null && SpeechRecognitionManager.isVoiceRecognitionActive(serverPlayer)) {
            SpeechRecognitionManager.updateConfiguration(serverPlayer, language, spells);
        }
    }
    
    // Utility
    @Override
    public String toString() {
        return String.format("Player{uuid=%s, spells=%d, language='%s'}", 
            uuid, spells.size(), language);
    }
} 
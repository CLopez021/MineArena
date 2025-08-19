package com.clopez021.mine_arena.player;

import com.clopez021.mine_arena.speech_recognition.SpeechCommand;
import com.clopez021.mine_arena.speech_recognition.SpeechRecognitionManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager class for handling player-specific data and configurations.
 * This class owns the SpeechRecognitionManager and handles all player management responsibilities.
 * Uses singleton pattern for proper state management.
 */
@Mod.EventBusSubscriber(modid = "mine_arena")
public class PlayerManager {
    
    private static PlayerManager instance;
    
    private final Map<UUID, List<String>> playerSpells = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerLanguages = new ConcurrentHashMap<>();
    
    // Default spell list - empty, players add their own
    private static final List<String> DEFAULT_SPELLS = List.of();
    // Default language - English
    private static final String DEFAULT_LANGUAGE = "en-US";
    
    private PlayerManager() {
        // Private constructor for singleton
    }
    
    /**
     * Gets the singleton instance of PlayerManager.
     * 
     * @return The PlayerManager instance
     */
    public static PlayerManager getInstance() {
        if (instance == null) {
            synchronized (PlayerManager.class) {
                if (instance == null) {
                    instance = new PlayerManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Gets the current spell list for a player.
     * 
     * @param player The ServerPlayer to get spells for
     * @return List of spell phrases, or empty list if none configured
     */
    public List<String> getSpells(ServerPlayer player) {
        return new ArrayList<>(playerSpells.getOrDefault(player.getUUID(), DEFAULT_SPELLS));
    }
    
    /**
     * Gets the current language for a player.
     * 
     * @param player The ServerPlayer to get language for
     * @return Language code (e.g., "en-US")
     */
    public String getLanguage(ServerPlayer player) {
        return playerLanguages.getOrDefault(player.getUUID(), DEFAULT_LANGUAGE);
    }
    
    /**
     * Sets the spell list for a player.
     * 
     * @param player The ServerPlayer to set spells for
     * @param spells List of spell phrases
     */
    public void setSpells(ServerPlayer player, List<String> spells) {
        UUID playerId = player.getUUID();
        playerSpells.put(playerId, new ArrayList<>(spells));
        
        // Update speech recognition if active
        if (SpeechRecognitionManager.isVoiceRecognitionActive(player)) {
            SpeechRecognitionManager.updateConfiguration(player, getLanguage(player), spells);
        }
    }
    
    /**
     * Sets the language for a player.
     * 
     * @param player The ServerPlayer to set language for
     * @param language Language code (e.g., "en-US")
     */
    public void setLanguage(ServerPlayer player, String language) {
        UUID playerId = player.getUUID();
        playerLanguages.put(playerId, language);
        
        // Update speech recognition if active
        if (SpeechRecognitionManager.isVoiceRecognitionActive(player)) {
            SpeechRecognitionManager.updateConfiguration(player, language, getSpells(player));
        }
    }
    
    /**
     * Adds a spell to a player's recognition list.
     * 
     * @param player The ServerPlayer to add spell for
     * @param spell The spell phrase to add
     */
    public void addSpell(ServerPlayer player, String spell) {
        UUID playerId = player.getUUID();
        List<String> spells = playerSpells.computeIfAbsent(playerId, k -> new ArrayList<>(DEFAULT_SPELLS));
        
        if (!spells.contains(spell)) {
            spells.add(spell);
            
            // Update speech recognition if active
            if (SpeechRecognitionManager.isVoiceRecognitionActive(player)) {
                SpeechRecognitionManager.updateConfiguration(player, getLanguage(player), spells);
            }
        }
    }
    
    /**
     * Removes a spell from a player's recognition list.
     * 
     * @param player The ServerPlayer to remove spell for
     * @param spell The spell phrase to remove
     */
    public void removeSpell(ServerPlayer player, String spell) {
        UUID playerId = player.getUUID();
        List<String> spells = playerSpells.get(playerId);
        
        if (spells != null && spells.remove(spell)) {
            // Update speech recognition if active
            if (SpeechRecognitionManager.isVoiceRecognitionActive(player)) {
                SpeechRecognitionManager.updateConfiguration(player, getLanguage(player), spells);
            }
        }
    }
    
    /**
     * Starts voice recognition for a player with their current settings.
     * 
     * @param player The ServerPlayer to start voice recognition for
     */
    public void startVoiceRecognition(ServerPlayer player) {
        SpeechRecognitionManager.startVoiceRecognition(player, getSpells(player), getLanguage(player));
    }
    
    /**
     * Stops voice recognition for a player.
     * 
     * @param player The ServerPlayer to stop voice recognition for
     */
    public void stopVoiceRecognition(ServerPlayer player) {
        SpeechRecognitionManager.stopVoiceRecognition(player);
    }
    
    /**
     * Main entry point for handling recognized speech commands.
     * This is where speech recognition results are processed and can trigger
     * various player management actions.
     * 
     * @param player The ServerPlayer who spoke the command
     * @param command The recognized speech command
     */
    public void handleSpeechCommand(ServerPlayer player, SpeechCommand command) {
        String spell = command.getSpell();
        
        // Output to console for debugging
        System.out.printf("[Voice] Player %s cast spell: %s%n", 
            player.getName().getString(), spell);
        
        // Output to Minecraft chat
        String chatMessage = String.format("🎤 Spell Cast: %s", spell);
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(chatMessage));
        
        // TODO: Add spell-specific command handling here
        // This is where you would dispatch to different handlers based on the recognized spell
        // For example:
        // - Building commands ("place stone", "build wall")
        // - Navigation commands ("go home", "teleport to spawn")
        // - Inventory commands ("equip sword", "use potion")
    }
    
    /**
     * Cleans up all player data for a specific player.
     * 
     * @param player The ServerPlayer to clean up data for
     */
    private void cleanupPlayerData(ServerPlayer player) {
        UUID playerId = player.getUUID();
        playerSpells.remove(playerId);
        playerLanguages.remove(playerId);
    }
    
    /**
     * Event handler for player login.
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerManager manager = getInstance();
            
            // Initialize player with defaults if not already set
            if (!manager.playerSpells.containsKey(player.getUUID())) {
                manager.playerSpells.put(player.getUUID(), new ArrayList<>(DEFAULT_SPELLS));
            }
            if (!manager.playerLanguages.containsKey(player.getUUID())) {
                manager.playerLanguages.put(player.getUUID(), DEFAULT_LANGUAGE);
            }
            
            // Optionally auto-start voice recognition on login
            // manager.startVoiceRecognition(player);
        }
    }
    
    /**
     * Event handler for player logout - clean up player data and voice recognition.
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerManager manager = getInstance();
            manager.stopVoiceRecognition(player);
            manager.cleanupPlayerData(player);
        }
    }
    
    /**
     * Shuts down all player management and speech recognition.
     * Should be called during server shutdown.
     */
    public void shutdown() {
        SpeechRecognitionManager.shutdownAll();
        playerSpells.clear();
        playerLanguages.clear();
        System.out.println("Shut down all player management");
    }
    
    /**
     * Static convenience method for shutdown to maintain compatibility.
     */
    public static void shutdownAll() {
        if (instance != null) {
            instance.shutdown();
        }
    }
} 
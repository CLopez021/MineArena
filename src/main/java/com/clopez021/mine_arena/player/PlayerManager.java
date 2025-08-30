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
 * Simple router that manages Player objects by UUID.
 * Clean, minimal manager that delegates to Player model objects.
 * Uses singleton pattern for proper state management.
 */
@Mod.EventBusSubscriber(modid = "mine_arena")
public class PlayerManager {
    
    private static PlayerManager instance = new PlayerManager();
    
    // Simple dictionary: UUID -> Player
    private final Map<UUID, Player> players = new ConcurrentHashMap<>();
    
    private PlayerManager() {
        // Private constructor for singleton
    }
    
    /**
     * Gets the singleton instance of PlayerManager.
     * 
     * @return The PlayerManager instance
     */
    public static PlayerManager getInstance() {
        System.out.println("Getting instance of PlayerManager");
        if (instance == null) {
            System.out.println("Instance is null");
            synchronized (PlayerManager.class) {
                if (instance == null) {
                    instance = new PlayerManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Gets a Player object for the given ServerPlayer.
     * Returns null if no player exists.
     * 
     * @param serverPlayer The ServerPlayer
     * @return Player object or null if not found
     */
    private Player getPlayer(ServerPlayer serverPlayer) {
        return players.get(serverPlayer.getUUID());
    }
    
    /**
     * Creates a new Player object for the given ServerPlayer.
     * 
     * @param serverPlayer The ServerPlayer
     * @return The created Player object
     */
    public Player createPlayer(ServerPlayer serverPlayer) {
        Player player = new Player(serverPlayer);
        players.put(serverPlayer.getUUID(), player);
        return player;
    }
    

    
    /**
     * Sets the spell list for a player.
     * 
     * @param serverPlayer The ServerPlayer to set spells for
     * @param spells List of spell phrases
     */
    public void setSpells(ServerPlayer serverPlayer, java.util.Map<String, com.clopez021.mine_arena.data.PlayerSpell> spells) {
        Player player = getPlayer(serverPlayer);
        if (player != null) {
            player.setSpells(spells);
        }
    }

    // Backward-compatible overload: accept list and convert to phrase-keyed map
    public void setSpells(ServerPlayer serverPlayer, java.util.List<com.clopez021.mine_arena.data.PlayerSpell> spells) {
        Player player = getPlayer(serverPlayer);
        if (player != null) {
            java.util.Map<String, com.clopez021.mine_arena.data.PlayerSpell> map = new java.util.HashMap<>();
            for (com.clopez021.mine_arena.data.PlayerSpell ps : spells) {
                map.put(ps.phrase(), ps);
            }
            player.setSpells(map);
        }
    }
    
    /**
     * Sets the language for a player.
     * 
     * @param serverPlayer The ServerPlayer to set language for
     * @param language Language code (e.g., "en-US")
     */
    public void setLanguage(ServerPlayer serverPlayer, String language) {
        Player player = getPlayer(serverPlayer);
        if (player != null) {
            player.setLanguage(language);
        }
    }
    
    /**
     * Adds a spell to a player's recognition list.
     * 
     * @param serverPlayer The ServerPlayer to add spell for
     * @param spell The spell phrase to add
     */
    public void addSpell(ServerPlayer serverPlayer, String phrase, String entityDataFile) {
        Player player = getPlayer(serverPlayer);
        if (player != null) {
            player.addSpell(phrase, entityDataFile);
        }
    }

    /**
     * Adds a spell by full PlayerSpell object.
     */
    public void addSpell(ServerPlayer serverPlayer, com.clopez021.mine_arena.data.PlayerSpell spell) {
        Player player = getPlayer(serverPlayer);
        if (player != null) {
            player.addSpell(spell);
        }
    }
    
    /**
     * Removes a spell from a player's recognition list.
     * 
     * @param serverPlayer The ServerPlayer to remove spell for
     * @param spell The spell phrase to remove
     */
    public void removeSpell(ServerPlayer serverPlayer, String spell) {
        Player player = getPlayer(serverPlayer);
        if (player != null) {
            player.removeSpell(spell);
        }
    }
    
    /**
     * Starts voice recognition for a player.
     * 
     * @param serverPlayer The ServerPlayer to start voice recognition for
     */
    public void startVoiceRecognition(ServerPlayer serverPlayer) {
        Player player = getPlayer(serverPlayer);
        if (player != null) {
            player.startVoiceRecognition();
        }
    }
    
    /**
     * Stops voice recognition for a player.
     * 
     * @param serverPlayer The ServerPlayer to stop voice recognition for
     */
    public void stopVoiceRecognition(ServerPlayer serverPlayer) {
        Player player = getPlayer(serverPlayer);
        if (player != null) {
            player.stopVoiceRecognition();
        }
    }
    
    /**
     * Main entry point for handling recognized speech commands.
     * 
     * @param serverPlayer The ServerPlayer who spoke the command
     * @param command The recognized speech command
     */
    public void handleSpeechCommand(ServerPlayer serverPlayer, SpeechCommand command) {
        Player player = getPlayer(serverPlayer);
        if (player == null) return;
        
        String spell = command.getSpell();
        
        // Output to console for debugging
        System.out.printf("[Voice] Player %s cast spell: %s%n", 
            serverPlayer.getName().getString(), spell);
        
        // Output to Minecraft chat
        String chatMessage = String.format("🎤 Spell Cast: %s", spell);
        serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal(chatMessage));
        
        // TODO: Add spell-specific command handling here
        // This is where you would dispatch to different handlers based on the recognized spell
        // For example:
        // - Building commands ("place stone", "build wall")
        // - Navigation commands ("go home", "teleport to spawn")
        // - Inventory commands ("equip sword", "use potion")
    }
    

    
    /**
     * Event handler for player login.
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PlayerManager manager = getInstance();
            
            // Create player object if it doesn't exist
            Player player = manager.getPlayer(serverPlayer);
            if (player == null) {
                player = manager.createPlayer(serverPlayer);
            }
            
            // Auto-start voice recognition on login
            player.startVoiceRecognition();
        }
    }
    
    /**
     * Event handler for player logout - clean up voice recognition and player object.
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PlayerManager manager = getInstance();
            manager.stopVoiceRecognition(serverPlayer);
            // Remove player object from memory (data is already persisted to SavedData)
            manager.players.remove(serverPlayer.getUUID());
        }
    }
    
    /**
     * Shuts down all player management and speech recognition.
     * Should be called during server shutdown.
     */
    public void shutdown() {
        // Stop voice recognition for all players
        players.values().forEach(Player::stopVoiceRecognition);
        SpeechRecognitionManager.shutdownAll();
        players.clear();
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

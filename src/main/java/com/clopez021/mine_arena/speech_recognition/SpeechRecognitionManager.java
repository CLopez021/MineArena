package com.clopez021.mine_arena.speech_recognition;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager class for handling speech recognition integration with Minecraft events.
 * This class provides a convenient way to manage voice sidecars for players.
 */
@Mod.EventBusSubscriber(modid = "mine_arena")
public class SpeechRecognitionManager {
    
    private static final Map<UUID, List<String>> playerSpells = new ConcurrentHashMap<>();
    private static final String DEFAULT_LANGUAGE = "en-US";
    
    // Default spell list - empty, players add their own
    private static final List<String> DEFAULT_SPELLS = Arrays.asList();
    
    /**
     * Starts voice recognition for a player with default spells.
     * 
     * @param player The ServerPlayer to start voice recognition for
     */
    public static void startVoiceRecognition(ServerPlayer player) {
        startVoiceRecognition(player, DEFAULT_SPELLS, DEFAULT_LANGUAGE);
    }
    
    /**
     * Starts voice recognition for a player with custom spells.
     * 
     * @param player The ServerPlayer to start voice recognition for
     * @param spells List of spell phrases to recognize
     * @param language Language code (e.g., "en-US")
     */
    public static void startVoiceRecognition(ServerPlayer player, List<String> spells, String language) {
        UUID playerId = player.getUUID();
        playerSpells.put(playerId, new ArrayList<>(spells));
        
        try {
            VoiceSidecar sidecar = VoiceSidecar.getInstance(playerId);
            sidecar.start(spells, language, command -> handleSpeechCommand(player, command), player);
            
            System.out.println("Started voice recognition for player: " + player.getName().getString());
        } catch (Exception e) {
            System.err.println("Failed to start voice recognition for player " + player.getName().getString() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Stops voice recognition for a player.
     * 
     * @param player The ServerPlayer to stop voice recognition for
     */
    public static void stopVoiceRecognition(ServerPlayer player) {
        UUID playerId = player.getUUID();
        VoiceSidecar.removeInstance(playerId);
        playerSpells.remove(playerId);
        
        System.out.println("Stopped voice recognition for player: " + player.getName().getString());
    }
    
    /**
     * Updates the spell list for a player.
     * 
     * @param player The ServerPlayer to update spells for
     * @param spells New list of spell phrases
     */
    public static void updateSpells(ServerPlayer player, List<String> spells) {
        UUID playerId = player.getUUID();
        playerSpells.put(playerId, new ArrayList<>(spells));
        
        VoiceSidecar sidecar = VoiceSidecar.getInstance(playerId);
        if (sidecar.isRunning()) {
            sidecar.sendConfig(DEFAULT_LANGUAGE, spells);
        }
    }
    
    /**
     * Adds a spell to a player's recognition list.
     * Auto-starts voice recognition if not already running.
     * 
     * @param player The ServerPlayer to add spell for
     * @param spell The spell phrase to add
     */
    public static void addSpell(ServerPlayer player, String spell) {
        UUID playerId = player.getUUID();
        List<String> spells = playerSpells.computeIfAbsent(playerId, k -> new ArrayList<>(DEFAULT_SPELLS));
        
        if (!spells.contains(spell)) {
            spells.add(spell);
            
            // Auto-start voice recognition if not running
            VoiceSidecar sidecar = VoiceSidecar.getInstance(playerId);
            if (!sidecar.isRunning()) {
                startVoiceRecognition(player, spells, DEFAULT_LANGUAGE);
            } else {
                updateSpells(player, spells);
            }
        }
    }
    
    /**
     * Removes a spell from a player's recognition list.
     * 
     * @param player The ServerPlayer to remove spell for
     * @param spell The spell phrase to remove
     */
    public static void removeSpell(ServerPlayer player, String spell) {
        UUID playerId = player.getUUID();
        List<String> spells = playerSpells.get(playerId);
        
        if (spells != null && spells.remove(spell)) {
            updateSpells(player, spells);
        }
    }
    
    /**
     * Gets the current spell list for a player.
     * 
     * @param player The ServerPlayer to get spells for
     * @return List of spell phrases, or empty list if none configured
     */
    public static List<String> getSpells(ServerPlayer player) {
        return playerSpells.getOrDefault(player.getUUID(), Collections.emptyList());
    }
    
    /**
     * Handles a speech command recognized for a player.
     * Outputs the match to Minecraft chat.
     * 
     * @param player The ServerPlayer who spoke the command
     * @param command The recognized speech command
     */
    private static void handleSpeechCommand(ServerPlayer player, SpeechCommand command) {
        String spell = command.getSpell();
        String heard = command.getHeard();
        double confidence = command.getConfidence();
        String matchType = command.getMatchKind();
        
        // Output to console for debugging
        System.out.printf("[Voice] Player %s: '%s' -> %s (%s, conf: %.2f)%n", 
            player.getName().getString(), heard, spell, matchType, confidence);
        
        // Output to Minecraft chat
        String chatMessage = String.format("🎤 Voice: \"%s\" -> %s (%s)", 
            heard, spell, matchType);
        
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(chatMessage));
    }
    
    /**
     * Event handler for player login - automatically start voice recognition.
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Optionally auto-start voice recognition on login
            // startVoiceRecognition(player);
        }
    }
    
    /**
     * Event handler for player logout - clean up voice recognition resources.
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            stopVoiceRecognition(player);
        }
    }
    
    /**
     * Shuts down all active voice recognition instances.
     * Should be called during server shutdown.
     */
    public static void shutdownAll() {
        VoiceSidecar.getAllInstances().keySet().forEach(VoiceSidecar::removeInstance);
        playerSpells.clear();
        System.out.println("Shut down all voice recognition instances");
    }
} 
package com.clopez021.mine_arena.speech_recognition;

import com.clopez021.mine_arena.client.speech_recognition.VoiceSidecar;
import com.clopez021.mine_arena.packets.PacketHandler;
import com.clopez021.mine_arena.packets.VoiceSidecarConfigPacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager class for handling speech recognition functionality.
 * This class focuses solely on speech recognition operations.
 * Player management is handled by PlayerManager.
 */
public class SpeechRecognitionManager {
    
    // Track which players have active voice recognition
    private static final Set<UUID> activeVoiceRecognition = ConcurrentHashMap.newKeySet();
    
    /**
     * Starts voice recognition for a player with specified spells and language.
     * 
     * @param player The ServerPlayer to start voice recognition for
     * @param spells List of spell phrases to recognize
     * @param language Language code (e.g., "en-US")
     */
    public static void startVoiceRecognition(ServerPlayer player, List<String> spells, String language) {
        if (!isVoiceRecognitionActive(player)) {
            activeVoiceRecognition.add(player.getUUID());
            
            // Send config to client to start the sidecar
            PacketHandler.INSTANCE.send(new VoiceSidecarConfigPacket(language, spells), player.connection.getConnection());
            
            System.out.println("Started voice recognition for player: " + player.getName().getString() + 
                " with " + spells.size() + " spells in language: " + language);
        }
    }
    
    /**
     * Stops voice recognition for a player.
     * 
     * @param player The ServerPlayer to stop voice recognition for
     */
    public static void stopVoiceRecognition(ServerPlayer player) {
        if (isVoiceRecognitionActive(player)) {
            activeVoiceRecognition.remove(player.getUUID());
            // TODO: Send stop packet to client if needed
            System.out.println("Stopped voice recognition for player: " + player.getName().getString());
        }
    }
    
    /**
     * Updates the configuration for an active voice recognition session.
     * 
     * @param player The ServerPlayer to update configuration for
     * @param language New language code
     * @param spells New list of spell phrases
     */
    public static void updateConfiguration(ServerPlayer player, String language, List<String> spells) {
        if (isVoiceRecognitionActive(player)) {
            // Send updated config to client
            PacketHandler.INSTANCE.send(new VoiceSidecarConfigPacket(language, spells), player.connection.getConnection());
            
            System.out.println("Updated voice recognition config for player: " + player.getName().getString() + 
                " with " + spells.size() + " spells in language: " + language);
        }
    }
    
    /**
     * Checks if voice recognition is currently active for a player.
     * 
     * @param player The ServerPlayer to check
     * @return true if voice recognition is active, false otherwise
     */
    public static boolean isVoiceRecognitionActive(ServerPlayer player) {
        return activeVoiceRecognition.contains(player.getUUID());
    }
    
    /**
     * Shuts down all active voice recognition instances.
     * Should be called during server shutdown.
     */
    public static void shutdownAll() {
        activeVoiceRecognition.clear();
        System.out.println("Shut down all voice recognition instances");
    }
} 
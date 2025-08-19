package com.clopez021.mine_arena.speech_recognition;

import java.util.UUID;

/**
 * Represents a recognized speech command from the Web Speech API.
 */
public class SpeechCommand {
    private final UUID playerId;
    private final String spell;
    
    public SpeechCommand(UUID playerId, String spell) {
        this.playerId = playerId;
        this.spell = spell;
    }
    
    /**
     * Gets the UUID of the player who spoke the command.
     */
    public UUID getPlayerId() {
        return playerId;
    }
    
    /**
     * Gets the canonical spell phrase that was matched.
     */
    public String getSpell() {
        return spell;
    }
    
    @Override
    public String toString() {
        return String.format("SpeechCommand{playerId=%s, spell='%s'}", playerId, spell);
    }
}
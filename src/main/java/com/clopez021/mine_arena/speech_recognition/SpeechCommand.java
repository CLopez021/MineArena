package com.clopez021.mine_arena.speech_recognition;

import java.util.UUID;

/**
 * Represents a recognized speech command from the Web Speech API.
 */
public class SpeechCommand {
    private final UUID playerId;
    private final String spell;
    private final String heard;
    private final String matchKind;
    private final double distance;
    private final double confidence;
    private final long timestamp;
    
    public SpeechCommand(UUID playerId, String spell, String heard, String matchKind, 
                        double distance, double confidence, long timestamp) {
        this.playerId = playerId;
        this.spell = spell;
        this.heard = heard;
        this.matchKind = matchKind;
        this.distance = distance;
        this.confidence = confidence;
        this.timestamp = timestamp;
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
    
    /**
     * Gets the raw text that was heard by the speech recognition.
     */
    public String getHeard() {
        return heard;
    }
    
    /**
     * Gets the type of match ("exact" or "fuzzy").
     */
    public String getMatchKind() {
        return matchKind;
    }
    
    /**
     * Gets the normalized edit distance (0.0 to 1.0) for fuzzy matches.
     */
    public double getDistance() {
        return distance;
    }
    
    /**
     * Gets the confidence score from the speech recognition engine.
     * Returns -1 if confidence is not available.
     */
    public double getConfidence() {
        return confidence;
    }
    
    /**
     * Gets the timestamp when the command was recognized.
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return String.format("SpeechCommand{playerId=%s, spell='%s', heard='%s', matchKind='%s', distance=%.3f, confidence=%.2f, timestamp=%d}",
            playerId, spell, heard, matchKind, distance, confidence, timestamp);
    }
}
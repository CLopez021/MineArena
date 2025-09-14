package com.clopez021.mine_arena.speech_recognition;

import java.util.UUID;

/**
 * Represents a recognized speech command from the Web Speech API. Contains the originating playerId
 * and the recognized spellName.
 */
public class SpeechCommand {
  private final UUID playerId;
  private final String spellName;

  public SpeechCommand(UUID playerId, String spellName) {
    this.playerId = playerId;
    this.spellName = spellName;
  }

  /** Gets the UUID of the player who spoke the command. */
  public UUID getPlayerId() {
    return playerId;
  }

  /** Gets the recognized spell name. */
  public String getSpellName() {
    return spellName;
  }

  @Override
  public String toString() {
    return String.format("SpeechCommand{playerId=%s, spellName='%s'}", playerId, spellName);
  }
}

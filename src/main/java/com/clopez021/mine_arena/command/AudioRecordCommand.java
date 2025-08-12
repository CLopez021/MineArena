package com.clopez021.mine_arena.command;

import com.clopez021.mine_arena.audio.RecorderManager;
import com.clopez021.mine_arena.audio.LLMService;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

public class AudioRecordCommand {
    
    /**
     * Starts recording audio for the player who executed the command
     * @param command The executed command context
     * @return 1 for success, 0 for failure
     */
    protected static int startRecording(CommandContext<CommandSourceStack> command) {
        try {
            // Get the player who executed the command
            ServerPlayer player = command.getSource().getPlayerOrException();
            UUID playerUuid = player.getUUID();
            
            // Check if player is already being recorded
            if (RecorderManager.isRecording(playerUuid)) {
                command.getSource().sendFailure(Component.literal("Error: You are already being recorded! Please wait for the current 5-second recording to finish."));
                return 0;
            }
            
            // Start recording with file saving callback
            boolean success = RecorderManager.startRecording(playerUuid, 5, (mp3Data) -> {
                if (mp3Data != null) {
                    String filename = saveAudioToFile(mp3Data, playerUuid, "recording");
                    if (filename != null) {
                        command.getSource().sendSuccess(() -> Component.literal("Recording saved as: " + filename), false);
                    } else {
                        command.getSource().sendFailure(Component.literal("Failed to save recording to file"));
                    }
                } else {
                    command.getSource().sendFailure(Component.literal("Recording failed or no audio data captured"));
                }
            });
            
            if (success) {
                command.getSource().sendSuccess(() -> Component.literal("Started 5-second audio recording"), false);
                command.getSource().sendSuccess(() -> Component.literal("Recording will automatically save in 5 seconds..."), false);
                return 1;
            } else {
                command.getSource().sendFailure(Component.literal("Error: Failed to start recording. Make sure Simple Voice Chat is installed and working."));
                return 0;
            }
            
        } catch (Exception e) {
            command.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Starts recording audio for transcription (5-second recording + OpenRouter API)
     * @param command The executed command context
     * @return 1 for success, 0 for failure
     */
    protected static int startTranscription(CommandContext<CommandSourceStack> command) {
        try {
            // Check if LLM service is configured
            if (!LLMService.isConfigured()) {
                command.getSource().sendFailure(Component.literal("Error: " + LLMService.getConfigurationInstructions()));
                return 0;
            }
            
            // Get the player who executed the command
            ServerPlayer player = command.getSource().getPlayerOrException();
            UUID playerUuid = player.getUUID();
            
            // Check if player is already being recorded
            if (RecorderManager.isRecording(playerUuid)) {
                command.getSource().sendFailure(Component.literal("Error: You are already being recorded! Please wait for the current 5-second recording to finish."));
                return 0;
            }
            
            // Start recording with transcription callback
            boolean success = RecorderManager.startRecording(playerUuid, 5, (mp3Data) -> {
                if (mp3Data != null) {
                    // Convert to base64 and transcribe
                    String base64Audio = Base64.getEncoder().encodeToString(mp3Data);
                    LLMService.transcribeAudio(base64Audio, player.getName().getString())
                        .thenAccept(transcriptionResult -> {
                            if (transcriptionResult != null && !transcriptionResult.trim().isEmpty()) {
                                command.getSource().sendSuccess(() -> Component.literal("Transcription: \"" + transcriptionResult + "\""), false);
                            } else {
                                command.getSource().sendFailure(Component.literal("Transcription failed or returned empty result."));
                            }
                        })
                        .exceptionally(throwable -> {
                            command.getSource().sendFailure(Component.literal("Transcription failed: " + throwable.getMessage()));
                            return null;
                        });
                } else {
                    command.getSource().sendFailure(Component.literal("Recording failed or no audio data captured"));
                }
            });
            
            if (success) {
                command.getSource().sendSuccess(() -> Component.literal("Started 5-second audio recording for transcription..."), false);
                command.getSource().sendSuccess(() -> Component.literal("Please speak clearly. Transcription will appear when complete."), false);
                return 1;
            } else {
                command.getSource().sendFailure(Component.literal("Error: Failed to start recording. Make sure Simple Voice Chat is installed and working."));
                return 0;
            }
            
        } catch (Exception e) {
            command.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Saves MP3 audio data to a file
     * @param mp3Data The MP3 audio bytes
     * @param playerUuid The player's UUID for naming
     * @param type The type of recording (e.g., "recording", "transcription")
     * @return The filename if successful, null if failed
     */
    private static String saveAudioToFile(byte[] mp3Data, UUID playerUuid, String type) {
        try {
            // Create recordings directory if it doesn't exist
            Path recordingsDir = Paths.get("recordings");
            if (!Files.exists(recordingsDir)) {
                Files.createDirectories(recordingsDir);
            }

            // Generate filename with UUID and timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("%s_%s_%s.mp3", 
                type, playerUuid.toString().substring(0, 8), timestamp);

            // Write MP3 data to file
            File outputFile = new File(recordingsDir.toFile(), filename);
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                fos.write(mp3Data);
            }

            return filename;
        } catch (IOException e) {
            // Log error but don't crash
            System.err.println("Failed to save audio file: " + e.getMessage());
            return null;
        }
    }
} 
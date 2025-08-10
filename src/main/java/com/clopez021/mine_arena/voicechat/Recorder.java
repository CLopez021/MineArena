package com.clopez021.mine_arena.voicechat;

import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.mp3.Mp3Encoder;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import javax.sound.sampled.AudioFormat;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Recorder {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SAMPLE_RATE = 48000; // Voice chat uses 48kHz
    private static final int CHANNELS = 1; // Mono
    private static final int SAMPLE_SIZE_BITS = 16;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    private final UUID playerUuid;
    private final String commandContext;
    private final OpusDecoder decoder;
    private final List<short[]> pcmChunks; // Store PCM chunks instead of bytes
    private final String filename;
    private boolean isRecording;

    public Recorder(UUID playerUuid, String commandContext) throws Exception {
        this.playerUuid = playerUuid;
        this.commandContext = commandContext;
        this.decoder = RecorderManager.getApi().createDecoder(); // Each recorder gets its own stateful decoder
        this.pcmChunks = new ArrayList<>();
        this.isRecording = true;
        
        // Create filename with timestamp and command context - now using MP3!
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String safeCommand = commandContext.replaceAll("[^a-zA-Z0-9_-]", "_");
        this.filename = String.format("recording_%s_%s_%s.mp3", 
            playerUuid.toString().substring(0, 8), safeCommand, timestamp);
        
        // Create recordings directory if it doesn't exist
        Path recordingsDir = Paths.get("recordings");
        if (!Files.exists(recordingsDir)) {
            Files.createDirectories(recordingsDir);
        }
        
        LOGGER.info("Created recorder for player {} with filename: {}", playerUuid, filename);
    }

    /**
     * Decodes opus audio data to PCM
     * @param opusData The encoded opus audio data
     * @return PCM audio data as short array
     */
    public short[] decodeOpus(byte[] opusData) {
        if (!isRecording) {
            return new short[0];
        }
        
        try {
            return decoder.decode(opusData);
        } catch (Exception e) {
            LOGGER.error("Failed to decode opus data for player {}: {}", playerUuid, e.getMessage());
            return new short[0];
        }
    }

    /**
     * Writes PCM audio data to the chunks list
     * @param pcmData The PCM audio data
     */
    public void writePcm(short[] pcmData) {
        if (!isRecording || pcmData.length == 0) {
            return;
        }
        
        // Store PCM chunks directly - much more efficient!
        synchronized (pcmChunks) {
            pcmChunks.add(pcmData.clone()); // Clone to avoid reference issues
        }
    }

    /**
     * Stops recording and saves the audio to an MP3 file
     * @return The filename of the saved recording
     * @throws Exception If saving fails
     */
    public String stopRecording() throws Exception {
        if (!isRecording) {
            throw new IllegalStateException("Recording is not active");
        }
        
        isRecording = false;
        decoder.close();
        
        synchronized (pcmChunks) {
            if (pcmChunks.isEmpty()) {
                LOGGER.warn("No audio data recorded for player {}", playerUuid);
                return filename;
            }
            
            // Save as MP3 file using Voice Chat API
            saveAsMp3();
        }
        
        LOGGER.info("Saved recording for player {} as {}, {} PCM chunks processed", 
            playerUuid, filename, pcmChunks.size());
        
        return filename;
    }

    /**
     * Saves the PCM chunks as an MP3 file using Voice Chat API
     * @throws Exception If saving fails
     */
    private void saveAsMp3() throws Exception {
        // Create audio format for 48kHz, 16-bit, mono PCM
        AudioFormat audioFormat = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            SAMPLE_RATE, 
            SAMPLE_SIZE_BITS, 
            CHANNELS, 
            CHANNELS * SAMPLE_SIZE_BITS / 8, 
            SAMPLE_RATE, 
            false // little endian
        );
        
        try (FileOutputStream outputStream = new FileOutputStream(new File("recordings", filename))) {
            // Create MP3 encoder with good quality settings
            Mp3Encoder encoder = RecorderManager.getApi().createMp3Encoder(
                audioFormat, 
                128, // 128 kbps bitrate 
                2,   // Quality 2 (good balance of quality/size)
                outputStream
            );
            
            if (encoder == null) {
                throw new Exception("MP3 encoder not available (might be Bukkit environment)");
            }
            
            try {
                // Process each PCM chunk and encode to MP3
                for (short[] pcmChunk : pcmChunks) {
                    encoder.encode(pcmChunk);
                }
            } finally {
                // Ensure encoder is always closed, even if encoding fails
                try {
                    encoder.close();
                } catch (IOException e) {
                    LOGGER.warn("Failed to close MP3 encoder for player {}: {}", playerUuid, e.getMessage());
                    // Don't re-throw here - we want the original exception to bubble up
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to save MP3 file for player {}: {}", playerUuid, e.getMessage());
            throw e;
        }
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getCommandContext() {
        return commandContext;
    }

    public String getFilename() {
        return filename;
    }

    public boolean isRecording() {
        return isRecording;
    }

    /**
     * Schedules automatic stop after 5 seconds
     */
    public void scheduleAutoStop() {
        scheduler.schedule(() -> {
            if (isRecording) {
                try {
                    String savedFilename = stopRecording();
                    LOGGER.info("Auto-stopped recording for player {}, saved as: {}", playerUuid, savedFilename);
                    // Remove from active recorders
                    RecorderManager.removeRecorder(playerUuid);
                } catch (Exception e) {
                    LOGGER.error("Failed to auto-stop recording for player {}: {}", playerUuid, e.getMessage());
                }
            }
        }, 5, TimeUnit.SECONDS);
    }

    /**
     * Shuts down the scheduler (called during server shutdown)
     */
    public static void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
} 
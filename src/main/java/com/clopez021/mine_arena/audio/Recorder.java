package com.clopez021.mine_arena.audio;

import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.mp3.Mp3Encoder;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import javax.sound.sampled.AudioFormat;
import java.io.*;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

public class Recorder {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SAMPLE_RATE = 48000; // Voice chat uses 48kHz
    private static final int CHANNELS = 1; // Mono
    private static final int SAMPLE_SIZE_BITS = 16;
    
    private final UUID playerUuid;
    private final OpusDecoder decoder;
    private final List<short[]> pcmChunks; // Store PCM chunks instead of bytes
    private boolean isRecording;

    public Recorder(UUID playerUuid) throws Exception {
        this.playerUuid = playerUuid;
        this.decoder = RecorderManager.getApi().createDecoder(); // Each recorder gets its own stateful decoder
        this.pcmChunks = new ArrayList<>();
        this.isRecording = true;
        
        LOGGER.info("Created recorder for player {}", playerUuid);
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
     * Stops recording
     * @return true if recording was active and stopped successfully
     * @throws Exception If stopping fails
     */
    public boolean stopRecording() throws Exception {
        if (!isRecording) {
            return false;
        }
        
        isRecording = false;
        decoder.close();
        
        LOGGER.info("Stopped recording for player {}, {} PCM chunks captured", 
            playerUuid, pcmChunks.size());
        
        return true;
    }

    /**
     * Converts the recorded PCM chunks to MP3 bytes
     * @return MP3 audio data as byte array, or null if no audio recorded
     */
    public byte[] getMp3Bytes() {
        synchronized (pcmChunks) {
            if (pcmChunks.isEmpty()) {
                LOGGER.warn("No audio data recorded for player {}", playerUuid);
                return null;
            }

            try {
                return convertPcmToMp3Bytes();
            } catch (Exception e) {
                LOGGER.error("Failed to convert PCM to MP3 bytes for player {}: {}", playerUuid, e.getMessage());
                return null;
            }
        }
    }

    /**
     * Encodes PCM chunks to MP3 using the provided output stream
     * @param outputStream The output stream to write MP3 data to
     * @throws Exception If encoding fails
     */
    private void encodePcmToMp3(OutputStream outputStream) throws Exception {
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
    }

    /**
     * Converts PCM chunks to MP3 bytes in memory (no file saving)
     * @return MP3 audio data as byte array
     * @throws Exception If conversion fails
     */
    private byte[] convertPcmToMp3Bytes() throws Exception {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            encodePcmToMp3(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            LOGGER.error("Failed to convert PCM to MP3 bytes for player {}: {}", playerUuid, e.getMessage());
            throw e;
        }
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public int getPcmChunksSize() {
        synchronized (pcmChunks) {
            return pcmChunks.size();
        }
    }
} 
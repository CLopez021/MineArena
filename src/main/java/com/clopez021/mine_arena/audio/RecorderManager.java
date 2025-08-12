package com.clopez021.mine_arena.audio;

import de.maxhenkel.voicechat.api.VoicechatApi;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class RecorderManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static VoicechatApi voicechatApi;
    private static final Map<UUID, RecordingJob<byte[]>> activeJobs = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    /**
     * Container for a recording job with CompletableFuture-based completion
     * @param <T> The type of data produced when recording completes
     */
    private static class RecordingJob<T> {
        final Recorder recorder; // The recorder instance
        final CompletableFuture<T> result; // The future that will be completed with the result
        final ScheduledFuture<?> timeout; // The scheduled future that will be used to cancel the recording, 
                                         // assuming we dont complete earlier
        final Supplier<T> producer; // The supplier that will be used to produce the result
        
        RecordingJob(Recorder recorder, CompletableFuture<T> result, 
                     ScheduledFuture<?> timeout, Supplier<T> producer) {
            this.recorder = recorder;
            this.result = result;
            this.timeout = timeout;
            this.producer = producer; 
        }
        
        /**
         * Completes the job by stopping recording and producing the result
         * This method is idempotent - can be called multiple times safely
         */
        void complete() {
            try {
                if (recorder.isRecording()) {
                    recorder.stopRecording();
                }
                T data = producer.get();
                
                // This will only succeed the first time (idempotent)
                boolean wasCompleted = result.complete(data);
                if (wasCompleted) {
                    LOGGER.info("Recording job completed for player {}, {} PCM chunks processed", 
                        recorder.getPlayerUuid(), recorder.getPcmChunksSize());
                }
            } catch (Exception e) {
                LOGGER.error("Failed to complete recording job for player {}: {}", 
                    recorder.getPlayerUuid(), e.getMessage());
                // Signal failure - also idempotent
                result.complete(null);
            }
        }
    }

    public static void init(VoicechatApi api) {
        voicechatApi = api;
        LOGGER.info("RecorderManager initialized with VoiceChat API");
    }

    public static VoicechatApi getApi() {
        return voicechatApi;
    }

    /**
     * Starts recording with a callback that will be executed after the specified duration
     * @param playerUuid The UUID of the player to record
     * @param durationSeconds How long to record for
     * @param onComplete Callback function that receives the MP3 bytes when recording completes
     * @return true if recording started successfully
     */
    public static boolean startRecording(UUID playerUuid, int durationSeconds, Consumer<byte[]> onComplete) {
        if (activeJobs.containsKey(playerUuid)) {
            LOGGER.warn("Player {} is already being recorded", playerUuid);
            return false;
        }

        try {
            Recorder recorder = new Recorder(playerUuid);
            CompletableFuture<byte[]> result = new CompletableFuture<>();
            // Attach callback to the future - this ensures exactly-once execution
            result.whenComplete((mp3Data, throwable) -> {
                if (throwable != null) {
                    LOGGER.error("Recording future completed exceptionally for player {}: {}", playerUuid, throwable.getMessage());
                    onComplete.accept(null);
                } else {
                    onComplete.accept(mp3Data);
                }
            });
            LOGGER.info("Started {}-second recording for player {}", durationSeconds, playerUuid);
            
            // Schedule the timeout completion
            ScheduledFuture<?> timeout = scheduler.schedule(() -> {
                RecordingJob<byte[]> job = activeJobs.remove(playerUuid);
                if (job != null) {
                    job.complete(); // This is the SAME function that early stop will call
                }
            }, durationSeconds, TimeUnit.SECONDS);
            
            // Create the job with the producer function
            RecordingJob<byte[]> job = new RecordingJob<>(
                recorder,
                result,
                timeout,
                recorder::getMp3Bytes // Producer function
            );
            
            // Store the job
            activeJobs.put(playerUuid, job);
            
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to start recording for player {}: {}", playerUuid, e.getMessage());
            return false;
        }
    }

    /**
     * Stops recording for a specific player (early stop)
     * This will cancel the scheduled timeout and complete the job immediately
     * @param playerUuid The UUID of the player to stop recording
     * @return true if recording was stopped successfully
     */
    public static boolean stopRecording(UUID playerUuid) {
        RecordingJob<byte[]> job = activeJobs.remove(playerUuid);
        
        if (job == null) {
            LOGGER.warn("No active recording found for player {}", playerUuid);
            return false;
        }

        try {
            // Cancel the scheduled timeout since we're stopping early
            if (!job.timeout.isDone()) {
                job.timeout.cancel(false);
                LOGGER.info("Cancelled scheduled timeout for early stop of player {}", playerUuid);
            }
            
            // Execute the SAME completion logic that the timeout would have called
            job.complete(); // This is idempotent and triggers the callback via CompletableFuture
            
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to stop recording for player {}: {}", playerUuid, e.getMessage());
            
            // Signal failure through the future
            job.result.complete(null);
            
            return false;
        }
    }

    /**
     * Checks if a player is currently being recorded
     * @param playerUuid The UUID of the player to check
     * @return true if the player is being recorded
     */
    public static boolean isRecording(UUID playerUuid) {
        return activeJobs.containsKey(playerUuid);
    }

    /**
     * Gets the recorder for a specific player
     * @param playerUuid The UUID of the player
     * @return The recorder instance, or null if not found
     */
    public static Recorder getRecorder(UUID playerUuid) {
        RecordingJob<byte[]> job = activeJobs.get(playerUuid);
        return job != null ? job.recorder : null;
    }

    /**
     * Stops all active recordings (useful for server shutdown)
     */
    public static void stopAllRecordings() {
        // Get all player UUIDs to avoid concurrent modification
        UUID[] playerUuids = activeJobs.keySet().toArray(new UUID[0]);
        
        for (UUID playerUuid : playerUuids) {
            try {
                stopRecording(playerUuid); // This will handle callback execution and cleanup
                LOGGER.info("Emergency stopped recording for player {}", playerUuid);
            } catch (Exception e) {
                LOGGER.error("Failed to emergency stop recording for player {}: {}", playerUuid, e.getMessage());
            }
        }
        
        // Ensure map is cleared (should already be clear from stopRecording calls)
        activeJobs.clear();
        
        // Shutdown the scheduler
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
package com.clopez021.mine_arena.voice.recording;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import java.util.UUID;

@ForgeVoicechatPlugin
public class VoiceChatPlugin implements VoicechatPlugin {

  @Override
  public String getPluginId() {
    return "mine_arena_recorder";
  }

  @Override
  public void initialize(VoicechatApi api) {
    RecorderManager.init(api);
  }

  @Override
  public void registerEvents(EventRegistration registration) {
    // Register microphone packet event for recording player audio
    registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
  }

  private void onMicrophonePacket(MicrophonePacketEvent event) {
    var senderConnection = event.getSenderConnection();
    if (senderConnection == null) return;

    UUID playerUuid = senderConnection.getPlayer().getUuid();

    // Check if this player is being recorded
    if (!RecorderManager.isRecording(playerUuid)) return;

    byte[] opusData = event.getPacket().getOpusEncodedData();
    if (opusData == null) return;

    // Get or create recorder for this player
    Recorder recorder = RecorderManager.getRecorder(playerUuid);
    if (recorder != null) {
      // Decode opus audio to PCM and write to recorder
      short[] pcmData = recorder.decodeOpus(opusData);
      recorder.writePcm(pcmData);
    }
  }
}

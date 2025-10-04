package com.clopez021.mine_arena.network;

import com.clopez021.mine_arena.voice.recognition.client.VoiceSidecar;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

/**
 * Server -> Client: Start sidecar or update its config (lang/spells). Sends language and a mapping
 * of phrase -> spell name.
 */
public class VoiceSidecarConfigPacket {
  private final String lang;
  private final Map<String, String> phraseToName;

  public VoiceSidecarConfigPacket(String lang, Map<String, String> phraseToName) {
    this.lang = lang;
    // defensive copy
    this.phraseToName =
        phraseToName != null ? new java.util.HashMap<>(phraseToName) : java.util.Map.of();
  }

  public void encode(FriendlyByteBuf buf) {
    buf.writeUtf(lang != null ? lang : "en-US");
    buf.writeVarInt(phraseToName.size());
    for (Map.Entry<String, String> e : phraseToName.entrySet()) {
      buf.writeUtf(e.getKey() != null ? e.getKey() : "");
      buf.writeUtf(e.getValue() != null ? e.getValue() : "");
    }
  }

  public static VoiceSidecarConfigPacket decode(FriendlyByteBuf buf) {
    String lang = buf.readUtf();
    int n = buf.readVarInt();
    java.util.Map<String, String> m = new java.util.HashMap<>();
    for (int i = 0; i < n; i++) {
      String phrase = buf.readUtf();
      String name = buf.readUtf();
      if (phrase != null && !phrase.isBlank() && name != null && !name.isBlank())
        m.put(phrase, name);
    }
    return new VoiceSidecarConfigPacket(lang, m);
  }

  public void handle(CustomPayloadEvent.Context ctx) {
    ctx.enqueueWork(
        () ->
            DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () ->
                    () -> {
                      var player = Minecraft.getInstance().player;
                      if (player == null) return;
                      try {
                        var sidecar = VoiceSidecar.getInstance();
                        if (!sidecar.isRunning()) {
                          sidecar.start(phraseToName, lang);
                        } else {
                          sidecar.sendConfig(lang, phraseToName);
                        }
                      } catch (Exception e) {
                        e.printStackTrace();
                      }
                    }));
    ctx.setPacketHandled(true);
  }
}

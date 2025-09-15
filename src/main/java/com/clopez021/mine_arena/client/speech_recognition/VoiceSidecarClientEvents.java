package com.clopez021.mine_arena.client.speech_recognition;

import com.clopez021.mine_arena.MineArena;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only events to manage the voice sidecar lifecycle when the client disconnects. */
@Mod.EventBusSubscriber(
    modid = MineArena.MOD_ID,
    bus = Mod.EventBusSubscriber.Bus.FORGE,
    value = Dist.CLIENT)
public final class VoiceSidecarClientEvents {
  private VoiceSidecarClientEvents() {}

  @SubscribeEvent
  public static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
    try {
      VoiceSidecar.getInstance().stop();
    } catch (Exception ignored) {
    }
  }
}

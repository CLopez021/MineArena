package com.clopez021.mine_arena.client;

import com.clopez021.mine_arena.MineArena;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class WandScreens {
  private static boolean timeoutActive = false;
  private static long timeoutDeadlineMs = 0L;

  public static void openWandScreen() {
    Minecraft.getInstance().setScreen(new WandSpellForm());
  }

  public static void startLoadingWithTimeout(long timeoutMs) {
    SpellLoadingIcon.startBrewing();
    timeoutActive = true;
    timeoutDeadlineMs = System.currentTimeMillis() + Math.max(0, timeoutMs);
    // Chat message (purple): brewing may take a while
    Minecraft mc = Minecraft.getInstance();
    if (mc.player != null) {
      mc.player.displayClientMessage(
          Component.literal("Brewing spell — may take a while")
              .withStyle(ChatFormatting.LIGHT_PURPLE),
          true);
    }
  }

  public static void onSpellCompleteClient() {
    SpellLoadingIcon.stopBrewing();
    timeoutActive = false;
    Minecraft mc = Minecraft.getInstance();
    if (mc.player != null) {
      mc.player.displayClientMessage(
          Component.literal("Spell completed").withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }
  }

  public static void onSpellErrorClient(String message) {
    SpellLoadingIcon.stopBrewing();
    timeoutActive = false;
    Minecraft mc = Minecraft.getInstance();
    if (mc.player != null) {
      mc.player.displayClientMessage(
          Component.literal(message != null ? message : "Spell failed.")
              .withStyle(ChatFormatting.RED),
          true);
    }
  }

  @Mod.EventBusSubscriber(
      modid = MineArena.MOD_ID,
      value = Dist.CLIENT,
      bus = Mod.EventBusSubscriber.Bus.FORGE)
  public static class ClientTicker {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
      if (event.phase != TickEvent.Phase.END) return;
      if (!timeoutActive) return;
      if (System.currentTimeMillis() >= timeoutDeadlineMs) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
          mc.player.displayClientMessage(Component.literal("Spell timed out."), true);
        }
        SpellLoadingIcon.stopBrewing();
        timeoutActive = false;
      }
    }
  }
}

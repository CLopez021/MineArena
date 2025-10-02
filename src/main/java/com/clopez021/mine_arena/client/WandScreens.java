package com.clopez021.mine_arena.client;

import com.clopez021.mine_arena.MineArena;
import com.clopez021.mine_arena.packets.PacketHandler;
import com.clopez021.mine_arena.packets.RequestSpellListPacket;
import com.clopez021.mine_arena.packets.SpellInfoListPacket;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

public class WandScreens {
  private static boolean timeoutActive = false;
  private static long timeoutDeadlineMs = 0L;
  private static List<SpellInfoListPacket.SpellInfo> spellList = new ArrayList<>();

  public static void openWandScreen() {
    // Request latest spell list from server
    PacketHandler.INSTANCE.send(new RequestSpellListPacket(), PacketDistributor.SERVER.noArg());
    Minecraft.getInstance().setScreen(new WandSpellForm(spellList));
  }

  public static void setSpellList(List<SpellInfoListPacket.SpellInfo> spells) {
    spellList = new ArrayList<>(spells);

    // Update the current screen if it's still open
    Minecraft mc = Minecraft.getInstance();
    if (mc.screen instanceof WandSpellForm form) {
      form.updateSpellList(spells);
    }
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

package com.clopez021.mine_arena.client.ui;

import com.clopez021.mine_arena.MineArena;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MineArena.MOD_ID, value = Dist.CLIENT)
public final class SpellLoadingIcon {
  private static final ResourceLocation CAULDRON =
      ResourceLocation.fromNamespaceAndPath(
          MineArena.MOD_ID, "textures/screen/cauldron_sprite_sheet.png");

  // Sprite sheet parameters
  private static final int FRAME_WIDTH = 256; // one frame width
  private static final int FRAME_HEIGHT = 256; // one frame height
  private static final int TOTAL_FRAMES = 24; // total frames in the sheet
  private static final int FRAME_TIME_TICKS = 2; // 20 tps -> 10 fps

  // Layout and style (no magic numbers in-line)
  private static final float ICON_SCALE = 0.10f; // ~10x smaller
  private static final int HUD_MARGIN = 8; // edge margin
  private static final int TEXT_ICON_GAP = 6; // gap between text and icon
  private static final int COLOR_PURPLE = 0xCBC3E3; // brewing text color

  // Simple brewing flag; no hidden/completed state tracking here
  private static volatile boolean BREWING = false;

  public static void startBrewing() {
    BREWING = true;
  }

  public static void stopBrewing() {
    BREWING = false;
  }

  @SubscribeEvent
  public static void onChatOverlay(CustomizeGuiOverlayEvent.Chat e) {
    if (!BREWING) return;

    GuiGraphics g = e.getGuiGraphics();
    Minecraft mc = Minecraft.getInstance();

    int sw = mc.getWindow().getGuiScaledWidth();

    // Overlay label kept minimal
    String label = "Brewing ...";

    // Animation frame selection
    long ticks = mc.level != null ? mc.level.getGameTime() : (System.currentTimeMillis() / 50L);
    int frame = (int) ((ticks / FRAME_TIME_TICKS) % TOTAL_FRAMES);
    int u = 0;
    int v = frame * FRAME_HEIGHT;

    int iconW = Math.round(FRAME_WIDTH * ICON_SCALE);
    int iconH = Math.round(FRAME_HEIGHT * ICON_SCALE);

    // Icon at top-right
    int xIcon = sw - iconW - HUD_MARGIN;
    int yIcon = HUD_MARGIN;

    // Text to the left of the icon, vertically centered with icon rect
    int textWidth = mc.font.width(label);
    int xText = xIcon - TEXT_ICON_GAP - textWidth;
    int yText = yIcon + Math.max(0, (iconH - mc.font.lineHeight) / 2);

    // Draw text first
    g.drawString(mc.font, label, xText, yText, COLOR_PURPLE, true);

    // Draw scaled sprite
    var pose = g.pose();
    pose.pushPose();
    pose.translate(xIcon, yIcon, 0);
    pose.scale(ICON_SCALE, ICON_SCALE, 1f);
    // Last two args are the FULL sheet size (w, h*frames)
    g.blit(
        CAULDRON, 0, 0, u, v, FRAME_WIDTH, FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT * TOTAL_FRAMES);
    pose.popPose();
  }
}

package com.clopez021.mine_arena;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = MineArena.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ServerConfig {
  private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

  // No player data stored in config anymore - moved to SavedData

  static final ForgeConfigSpec SPEC = BUILDER.build();

  @SubscribeEvent
  static void onLoad(final ModConfigEvent event) {
    // Config loading - no player data to load anymore
  }
}

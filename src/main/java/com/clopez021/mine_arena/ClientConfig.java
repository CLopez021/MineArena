package com.clopez021.mine_arena;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = MineArena.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientConfig {
  private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

  // Client-side settings only
  public static String meshyTestKey = "msy_dummy_api_key_for_test_mode_12345678";
  public static String meshyApiKey;

  public static final ForgeConfigSpec.ConfigValue<String> MESHY_API_KEY =
      BUILDER.comment("Meshy API Key").define("meshyApiKey", "YOUR_API_KEY");

  static final ForgeConfigSpec SPEC = BUILDER.build();

  @SubscribeEvent
  static void onLoad(final ModConfigEvent event) {
    meshyApiKey = MESHY_API_KEY.get();
  }
}

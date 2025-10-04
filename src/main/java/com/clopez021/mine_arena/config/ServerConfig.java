package com.clopez021.mine_arena.config;

import com.clopez021.mine_arena.MineArena;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = MineArena.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ServerConfig {
  private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

  // No player data stored in config anymore - moved to SavedData

  // OpenRouter configuration
  public static String openrouterApiKey;
  public static String openrouterModel;

  // Meshy configuration
  public static String meshyApiKey;

  public static final ForgeConfigSpec.ConfigValue<String> OPENROUTER_API_KEY =
      BUILDER.comment("OpenRouter API Key").define("openrouterApiKey", "YOUR_OPENROUTER_API_KEY");

  public static final ForgeConfigSpec.ConfigValue<String> OPENROUTER_MODEL =
      BUILDER
          .comment("OpenRouter model name for chat, e.g., openai/gpt-4o")
          .define("openrouterModel", "x-ai/grok-4-fast:free");

  public static final ForgeConfigSpec.ConfigValue<String> MESHY_API_KEY =
      BUILDER.comment("Meshy API Key").define("meshyApiKey", "YOUR_API_KEY");

  public static final ForgeConfigSpec SPEC = BUILDER.build();

  @SubscribeEvent
  static void onLoad(final ModConfigEvent event) {
    // Only handle our server config spec
    if (event.getConfig().getSpec() == SPEC) {
      openrouterApiKey = OPENROUTER_API_KEY.get();
      openrouterModel = OPENROUTER_MODEL.get();
      meshyApiKey = MESHY_API_KEY.get();
    }
  }
}

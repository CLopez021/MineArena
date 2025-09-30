package com.clopez021.mine_arena;

import com.clopez021.mine_arena.client.SpellEntityRenderer;
import com.clopez021.mine_arena.command.ModelCommand;
import com.clopez021.mine_arena.command.arguments.ModCommandArguments;
import com.clopez021.mine_arena.entity.ModEntities;
import com.clopez021.mine_arena.items.ModItems;
import com.clopez021.mine_arena.models.Model;
import com.clopez021.mine_arena.packets.PacketHandler;
import com.clopez021.mine_arena.player.PlayerManager;
import com.clopez021.mine_arena.spell.PlayerSpellConfig;
import com.clopez021.mine_arena.spell.SpellEntityConfig;
import com.clopez021.mine_arena.spell.behavior.onCollision.SpellEffectBehaviorConfig;
import com.clopez021.mine_arena.utils.ModelUtils;
import com.clopez021.mine_arena.voicechat.RecorderManager;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(MineArena.MOD_ID)
public class MineArena {
  // Define mod id in a common place for everything to reference
  public static final String MOD_ID = "mine_arena";

  @Nullable public static Model model;

  // Directly reference a slf4j logger
  private static final Logger LOGGER = LogUtils.getLogger();

  // Centralized default spells registry
  private static final List<PlayerSpellConfig> DEFAULT_SPELLS = new ArrayList<>();

  public MineArena(FMLJavaModLoadingContext context) {
    IEventBus modEventBus = context.getModEventBus();

    // Register ourselves for server and other game events we are interested in
    MinecraftForge.EVENT_BUS.register(this);

    ModItems.register(modEventBus);
    ModCommandArguments.register(modEventBus);
    ModEntities.register(modEventBus);
    PacketHandler.init();

    // Add items to creative tabs
    modEventBus.addListener(this::addCreative);

    // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
    context.registerConfig(ModConfig.Type.COMMON, ServerConfig.SPEC);
    context.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    // Defer default spell creation until server start (resources ready)
  }

  // You can use SubscribeEvent and let the Event Bus discover methods to call
  @SubscribeEvent
  public void onServerStarting(ServerStartingEvent event) {
    createDefaultSpellsFunction();
  }

  @SubscribeEvent
  public void onServerStopping(ServerStoppingEvent event) {
    // Clean up any active recordings when server stops
    RecorderManager.stopAllRecordings();
    // Clean up player management and speech recognition instances
    PlayerManager.shutdownAll();
    // Clear defaults to avoid stale state between dev runs
    DEFAULT_SPELLS.clear();
  }

  // You can use EventBusSubscriber to automatically register all static methods in the class
  // annotated with @SubscribeEvent
  @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
  public static class ClientModEvents {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
      EntityRenderers.register(ModEntities.SPELL_ENTITY.get(), SpellEntityRenderer::new);
    }
  }

  private void addCreative(BuildCreativeModeTabContentsEvent event) {
    if (event.getTabKey() == CreativeModeTabs.COMBAT
        || event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
      event.accept(ModItems.WAND);
    }
  }

  // ---- Default Spells API ----

  /** Build and cache default spells for new players. Run at server start. */
  public static void createDefaultSpellsFunction() {
    try {
      DEFAULT_SPELLS.clear();

      try {
        // Load assets from the mod's resources on the server side by extracting to models/
        ResourceLocation fireball_dir =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "models/fireball");
        String fireball_baseName = "fireball";
        Model fireball_model = ModelUtils.loadModelFromResources(fireball_dir, fireball_baseName);
        Map<BlockPos, BlockState> fireball_blocks = ModelCommand.buildVoxels(fireball_model);
        SpellEffectBehaviorConfig fireball_behavior =
            new SpellEffectBehaviorConfig(
                5.0f, // radius
                20.0f, // damage
                true, // despawnOnTrigger
                "minecraft:fire", // spawnId
                5, // spawnCount
                "ignite", // statusEffectId
                2, // statusDurationSeconds
                1, // statusAmplifier
                false, // affectOwner
                SpellEffectBehaviorConfig.EffectTrigger.ON_IMPACT, // trigger
                2.0f, // knockbackAmount
                3.0f, // blockDestructionRadius
                2); // blockDestructionDepth
        // Fireball: explosion + ignite effect for 5s
        SpellEntityConfig fireball_cfg =
            new SpellEntityConfig(fireball_blocks, 0.05f, fireball_behavior, true, 1.0f);
        DEFAULT_SPELLS.add(new PlayerSpellConfig("fireball", "fireball", fireball_cfg));

        // Shockwave: push entities away (pure knockback), no damage
        ResourceLocation wind_dir = ResourceLocation.fromNamespaceAndPath(MOD_ID, "models/wind");
        String wind_baseName = "wind";
        Model wind_model = ModelUtils.loadModelFromResources(wind_dir, wind_baseName);
        Map<BlockPos, BlockState> wind_blocks = ModelCommand.buildVoxels(wind_model);
        SpellEffectBehaviorConfig wind_behavior =
            new SpellEffectBehaviorConfig(
                4.0f, // radius
                0f, // damage
                true, // despawnOnTrigger
                "", // spawnId
                0, // spawnCount
                "", // statusEffectId (none)
                0, // statusDurationSeconds (none)
                0, // statusAmplifier (none)
                false, // affectOwner
                SpellEffectBehaviorConfig.EffectTrigger.ON_IMPACT, // trigger
                2.0f, // knockbackAmount (shockwave effect)
                0.0f, // blockDestructionRadius (no block breaking)
                0); // blockDestructionDepth (no block breaking)
        SpellEntityConfig wind_cfg =
            new SpellEntityConfig(wind_blocks, 0.05f, wind_behavior, true, 0.8f);
        DEFAULT_SPELLS.add(new PlayerSpellConfig("wind", "wind", wind_cfg));

        // Ice burst: place ice around and freeze nearby entities for 8s
        ResourceLocation ice_cube_dir =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "models/ice_cube");
        String ice_cube_baseName = "ice_cube";
        Model ice_cube_model = ModelUtils.loadModelFromResources(ice_cube_dir, ice_cube_baseName);
        ice_cube_model.rotation.rotateX(90);
        Map<BlockPos, BlockState> ice_cube_blocks = ModelCommand.buildVoxels(ice_cube_model);
        SpellEffectBehaviorConfig ice_cube_behavior =
            new SpellEffectBehaviorConfig(
                4.0f, // radius
                0f, // damage
                true, // despawnOnTrigger
                "minecraft:ice", // spawnId
                12, // spawnCount
                "freeze", // statusEffectId (freeze keyword)
                8, // statusDurationSeconds
                0, // statusAmplifier (unused for freeze)
                false, // affectOwner
                SpellEffectBehaviorConfig.EffectTrigger.ON_IMPACT, // trigger
                0.0f, // knockbackAmount
                0.0f, // blockDestructionRadius (no block breaking)
                0); // blockDestructionDepth (no block breaking)
        SpellEntityConfig ice_cube_cfg =
            new SpellEntityConfig(ice_cube_blocks, 0.05f, ice_cube_behavior, true, 0.6f);
        DEFAULT_SPELLS.add(new PlayerSpellConfig("ice_cube", "ice cube", ice_cube_cfg));

        // Rocket: faster, smaller radius but deals damage via explosion
        ResourceLocation bomb_dir = ResourceLocation.fromNamespaceAndPath(MOD_ID, "models/bomb");
        String bomb_baseName = "bomb";
        Model bomb_model = ModelUtils.loadModelFromResources(bomb_dir, bomb_baseName);
        Map<BlockPos, BlockState> bomb_blocks = ModelCommand.buildVoxels(bomb_model);
        SpellEffectBehaviorConfig bomb_behavior =
            new SpellEffectBehaviorConfig(
                2.5f, // radius
                6.0f, // damage
                true, // despawnOnTrigger
                "", // spawnId
                0, // spawnCount
                "", // statusEffectId
                0, // statusDurationSeconds
                1, // statusAmplifier
                false, // affectOwner
                SpellEffectBehaviorConfig.EffectTrigger.ON_IMPACT, // trigger
                1.0f, // knockbackAmount (explosive knockback)
                2.5f, // blockDestructionRadius (bomb breaks blocks)
                3); // blockDestructionDepth (deep destruction)
        SpellEntityConfig bomb_cfg =
            new SpellEntityConfig(bomb_blocks, 0.05f, bomb_behavior, true, 1.5f);
        DEFAULT_SPELLS.add(new PlayerSpellConfig("bomb", "bomb", bomb_cfg));

        // Levitate: on-cast effect applied to player
        SpellEffectBehaviorConfig levitate_behavior =
            new SpellEffectBehaviorConfig(
                4.0f, // radius
                0f, // damage
                true, // despawnOnTrigger
                "", // spawnId
                0, // spawnCount
                "minecraft:levitation", // statusEffectId
                5, // statusDurationSeconds
                10, // statusAmplifier
                true, // affectOwner
                SpellEffectBehaviorConfig.EffectTrigger.ON_CAST, // trigger
                0.0f, // knockbackAmount
                0.0f, // blockDestructionRadius (no block breaking)
                0); // blockDestructionDepth (no block breaking)
        SpellEntityConfig levitate_cfg =
            new SpellEntityConfig(Map.of(), 0.05f, levitate_behavior, true, 0.8f);
        DEFAULT_SPELLS.add(new PlayerSpellConfig("levitate", "levitate", levitate_cfg));
      } catch (Exception ex) {
        // If model loading fails, do not add a default spell
        LOGGER.warn("Failed to load default model from resources. No default spell added.", ex);
      }

      LOGGER.info("MineArena default spells initialized: {}", DEFAULT_SPELLS.size());
    } catch (Exception e) {
      LOGGER.error("Failed to create default spells", e);
    }
  }

  /** Returns an immutable view of the default spells to assign to players. */
  public static Collection<PlayerSpellConfig> getDefaultSpells() {
    System.out.println("getDefaultSpells: " + DEFAULT_SPELLS.size());
    return Collections.unmodifiableList(DEFAULT_SPELLS);
  }
}

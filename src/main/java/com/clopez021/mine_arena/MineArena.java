package com.clopez021.mine_arena;

import com.clopez021.mine_arena.command.arguments.ModCommandArguments;
import com.clopez021.mine_arena.entity.ModEntities;
import com.clopez021.mine_arena.items.ModItems;
import com.clopez021.mine_arena.models.Model;
import com.clopez021.mine_arena.packets.PacketHandler;
import com.clopez021.mine_arena.voicechat.RecorderManager;

import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.entity.EntityRenderers;
import com.clopez021.mine_arena.client.SpellEntityRenderer;
import com.clopez021.mine_arena.player.PlayerManager;
import com.clopez021.mine_arena.spell.PlayerSpellConfig;
import com.clopez021.mine_arena.spell.SpellEntityConfig;
import com.clopez021.mine_arena.command.ModelCommand;
import com.clopez021.mine_arena.utils.ModelUtils;
import com.clopez021.mine_arena.spell.behavior.onCollision.CollisionBehaviorConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
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

import javax.annotation.Nullable;
 
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
 

// The value here should match an entry in the META-INF/mods.toml file
@Mod(MineArena.MOD_ID)
public class MineArena {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "mine_arena";

    @Nullable
    public static Model model;

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
        context.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
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

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            EntityRenderers.register(ModEntities.SPELL_ENTITY.get(), SpellEntityRenderer::new);
        }
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT || event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.WAND);
        }
    }

    // ---- Default Spells API ----

    /**
     * Build and cache default spells for new players. Run at server start.
     */
    public static void createDefaultSpellsFunction() {
        try {
            DEFAULT_SPELLS.clear();

            try {
                // Load assets from the mod's resources on the server side by extracting to models/
                ResourceLocation dir = ResourceLocation.fromNamespaceAndPath(MOD_ID, "models/fireball");
                String baseName = "fireball";
                Model m = ModelUtils.loadModelFromResources(dir, baseName);
                Map<BlockPos, BlockState> blocks = ModelCommand.buildVoxels(m);
                CollisionBehaviorConfig behavior = new CollisionBehaviorConfig("explode", 10f, 5f, true, "minecraft:skeleton", 20);
                SpellEntityConfig cfg = new SpellEntityConfig(blocks, 0.4f, behavior, SpellEntityConfig.MovementDirection.FORWARD, 0.9f);
                DEFAULT_SPELLS.add(new PlayerSpellConfig("fireball", "fireball", cfg));
                LOGGER.info("Loaded default spell model from resources {}/{}", dir, baseName);
            } catch (Exception ex) {
                // If model loading fails, do not add a default spell
                LOGGER.warn("Failed to load default model from resources. No default spell added.", ex);
            }

            LOGGER.info("MineArena default spells initialized: {}", DEFAULT_SPELLS.size());
        } catch (Exception e) {
            LOGGER.error("Failed to create default spells", e);
        }
    }

    /**
     * Returns an immutable view of the default spells to assign to players.
     */
    public static Collection<PlayerSpellConfig> getDefaultSpells() {
        System.out.println("getDefaultSpells: " + DEFAULT_SPELLS.size());
        return Collections.unmodifiableList(DEFAULT_SPELLS);
    }

    
}

package com.clopez021.mine_arena;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.clopez021.mine_arena.command.arguments.ModCommandArguments;
import com.clopez021.mine_arena.entity.ModEntities;
import com.clopez021.mine_arena.items.ModItems;
import com.clopez021.mine_arena.models.Model;
import com.clopez021.mine_arena.models.util.Palette;
import com.clopez021.mine_arena.packets.PacketHandler;
import com.clopez021.mine_arena.voicechat.RecorderManager;

import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(MineArena.MOD_ID)
public class MineArena {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "mine_arena";

    @Nullable
    public static Model model;

    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public MineArena(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        ModItems.register(modEventBus);
        ModCommandArguments.register(modEventBus);
        ModEntities.register(modEventBus);
        PacketHandler.init();

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        context.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.MODEL_HAMMER);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        // Clean up any active recordings when server stops
        RecorderManager.stopAllRecordings();
        // Clean up player management and speech recognition instances
        com.clopez021.mine_arena.player.PlayerManager.shutdownAll();
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            EntityRenderers.register(com.clopez021.mine_arena.entity.ModEntities.MODEL_ENTITY.get(), com.clopez021.mine_arena.renderer.ModelEntityRenderer::new);
        }
    }
}
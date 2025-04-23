package com.knkevin.ai_builder.events;

import com.knkevin.ai_builder.AIBuilder;
import com.knkevin.ai_builder.command.ModelCommand;
import com.knkevin.ai_builder.models.util.Palette;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.server.command.ConfigCommand;

import java.io.File;

public class ModEvents {
    /**
     * Events fired on the Forge bus.
     */
    @Mod.EventBusSubscriber(modid = AIBuilder.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ModForgeEvents {
        @SubscribeEvent
        public static void registerCommands(final RegisterCommandsEvent event) {
            ModelCommand.register(event.getDispatcher());
            ConfigCommand.register(event.getDispatcher());
        }
    }

    /**
     * Events fired on the Mod bus.
     */
    @Mod.EventBusSubscriber(modid = AIBuilder.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModModEvents {
        @SubscribeEvent
        public static void commonSetup(final FMLCommonSetupEvent event) {
            Palette.loadPaletteFromJSON();
            File folder = new File("models");
            if (!folder.exists()) folder.mkdir();
        }
    }
}
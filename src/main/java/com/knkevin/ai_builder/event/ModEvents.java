package com.knkevin.ai_builder.event;

import com.knkevin.ai_builder.AIBuilder;
import com.knkevin.ai_builder.command.model.ModelCommand;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.command.ConfigCommand;

public class ModEvents {
    @Mod.EventBusSubscriber(modid = AIBuilder.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ModForgeEvents {
        @SubscribeEvent
        public static void registerCommands(final RegisterCommandsEvent event) {
            ModelCommand.register(event.getDispatcher());
            ConfigCommand.register(event.getDispatcher());
        }
    }
}
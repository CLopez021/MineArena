package com.clopez021.mine_arena.events;

import com.clopez021.mine_arena.renderer.BoxRenderer;
import com.clopez021.mine_arena.MineArena;
import com.clopez021.mine_arena.key_bindings.InputActions;
import com.clopez021.mine_arena.key_bindings.KeyBindings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Events fired on the clientside.
 */
public class ClientEvents {
    /**
     * Events fired on the Forge bus.
     */
    @Mod.EventBusSubscriber(modid = MineArena.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientForgeEvents {
        @SubscribeEvent
        public static void renderEvent(RenderLevelStageEvent event) {
            if (event.getStage().equals(RenderLevelStageEvent.Stage.AFTER_ENTITIES))
                BoxRenderer.renderEvent(event);
        }

        @SubscribeEvent
        public static void mouseScroll(InputEvent.MouseScrollingEvent event) {
            InputActions.mouseScrollEvent(event);
        }

        @SubscribeEvent
        public static void mouseButtonEvent(InputEvent.MouseButton event) {
            InputActions.mouseButtonEvent(event);
        }

        @SubscribeEvent
        public static void clientTickEvent(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END)
                InputActions.checkKeys();
        }
    }

    /**
     * Events fired on the Mod bus.
     */
    @Mod.EventBusSubscriber(modid = MineArena.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModBusEvents {
        @SubscribeEvent
        public static void registerKeyEvent(RegisterKeyMappingsEvent event) {
            event.register(KeyBindings.SCALE_KEY);
            event.register(KeyBindings.ROTATE_KEY);
            event.register(KeyBindings.TRANSLATE_KEY);
            event.register(KeyBindings.PLACE_KEY);
            event.register(KeyBindings.UNDO_KEY);
            event.register(KeyBindings.X_AXIS_KEY);
            event.register(KeyBindings.Y_AXIS_KEY);
            event.register(KeyBindings.Z_AXIS_KEY);
            event.register(KeyBindings.TOGGLE_VIEW_KEY);
        }
    }
}
package com.knkevin.ai_builder.key_bindings;

import com.knkevin.ai_builder.AIBuilder;
import com.knkevin.ai_builder.items.ModItems;
//import com.knkevin.ai_builder.packets.PacketHandler;
//import com.knkevin.ai_builder.packets.PlaceModelPacket;
//import com.knkevin.ai_builder.packets.UndoModelPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.InputEvent;
import org.joml.Vector3f;

import static com.knkevin.ai_builder.items.custom.HammerModes.*;
import static com.knkevin.ai_builder.key_bindings.KeyBindings.*;

public class KeyActions {



    public static void mouseScrollEvent(InputEvent.MouseScrollingEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !player.getMainHandItem().getItem().equals(ModItems.MODEL_HAMMER.get()) || AIBuilder.model == null) return;
        handleMouseScroll((float) event.getDeltaY());
        event.setCanceled(true);
    }

    public static void checkKeys() {
        Player player = Minecraft.getInstance().player;
        boolean holdingHammer = player != null && player.getMainHandItem().getItem().equals(ModItems.MODEL_HAMMER.get());
        boolean modelLoaded = AIBuilder.model != null;
        if (PLACE_KEY.consumeClick() && holdingHammer && modelLoaded) placeModel();
        if (UNDO_KEY.consumeClick() && holdingHammer && modelLoaded) undoModel();
        if (ROTATE_KEY.consumeClick() && holdingHammer) setModeRotate(player);
        if (SCALE_KEY.consumeClick() && holdingHammer) setModeScale(player);
        if (TRANSLATE_KEY.consumeClick() && holdingHammer) setModeTranslate(player);
        if (X_AXIS_KEY.consumeClick() && holdingHammer) setAxisX(player);
        if (Y_AXIS_KEY.consumeClick() && holdingHammer) setAxisY(player);
        if (Z_AXIS_KEY.consumeClick() && holdingHammer) setAxisZ(player);
        if (TOGGLE_VIEW_KEY.consumeClick() && holdingHammer) toggleViewMode(player);
    }

    public static void placeModel() {
        //PacketHandler.INSTANCE.sendToServer(new PlaceModelPacket());
    }

    public static void undoModel() {
        //PacketHandler.INSTANCE.sendToServer(new UndoModelPacket());
    }

    public static void setModeRotate(Player player) {
        transformMode = TransformMode.ROTATE;
        selectedAxis = Axis.Y;
        player.sendSystemMessage(Component.literal("Set mode to rotation."));
    }

    public static void setModeScale(Player player) {
        transformMode = TransformMode.SCALE;
        selectedAxis = Axis.ALL;
        player.sendSystemMessage(Component.literal("Set mode to scaling."));
    }

    public static void setModeTranslate(Player player) {
        transformMode = TransformMode.TRANSLATE;
        selectedAxis = Axis.Y;
        player.sendSystemMessage(Component.literal("Set mode to translation."));
    }

    public static void setAxisX(Player player) {
        selectedAxis = Axis.X;
        player.sendSystemMessage(Component.literal("Set axis to x-axis."));
    }

    public static void setAxisY(Player player) {
        selectedAxis = Axis.Y;
        player.sendSystemMessage(Component.literal("Set axis to y-axis."));
    }

    public static void setAxisZ(Player player) {
        selectedAxis = Axis.Z;
        player.sendSystemMessage(Component.literal("Set axis to z-axis."));
    }

    public static void toggleViewMode(Player player) {
        if (AIBuilder.model == null) return;
        if (viewMode == ViewMode.BOX) {
            viewMode = ViewMode.BLOCKS;
            AIBuilder.model.applyScale(1);
            player.sendSystemMessage(Component.literal("Viewing blocks preview."));
        } else {
            viewMode = ViewMode.BOX;
            player.sendSystemMessage(Component.literal("Viewing box outline."));
        }
    }

    public static void handleMouseScroll(float value) {
        if (AIBuilder.model == null) return;
        switch (transformMode) {
            case TRANSLATE -> AIBuilder.model.move(selectedAxis.name, value);
            case ROTATE -> AIBuilder.model.applyAxisRotation(selectedAxis.name, 5 * value);
            case SCALE -> {
                Vector3f size = new Vector3f(AIBuilder.model.maxCorner).sub(AIBuilder.model.minCorner);
                Vector3f scale = new Vector3f();
                if (selectedAxis == Axis.ALL)
                    scale.set(2/size.get(size.maxComponent())*value);
                else
                    scale.setComponent(selectedAxis.component, 2/(AIBuilder.model.maxCorner.get(selectedAxis.component) - AIBuilder.model.minCorner.get(selectedAxis.component)) * value);
                AIBuilder.model.setScale(AIBuilder.model.scale.x + scale.x, AIBuilder.model.scale.y + scale.y, AIBuilder.model.scale.z + scale.z);
            }
        }
    }
}
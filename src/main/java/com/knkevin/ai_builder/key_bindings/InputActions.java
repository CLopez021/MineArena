package com.knkevin.ai_builder.key_bindings;

import com.knkevin.ai_builder.AIBuilder;
import com.knkevin.ai_builder.items.ModItems;
//import com.knkevin.ai_builder.packets.PacketHandler;
//import com.knkevin.ai_builder.packets.PlaceModelPacket;
//import com.knkevin.ai_builder.packets.UndoModelPacket;
import com.knkevin.ai_builder.items.custom.ModelHammerItem;
import com.knkevin.ai_builder.packets.PacketHandler;
import com.knkevin.ai_builder.packets.PlaceModelPacket;
import com.knkevin.ai_builder.packets.UndoModelPacket;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;
import org.joml.Vector3f;

import java.io.IOException;

import static com.knkevin.ai_builder.items.custom.HammerModes.*;
import static com.knkevin.ai_builder.key_bindings.KeyBindings.*;

public class InputActions {
    public static void mouseScrollEvent(InputEvent.MouseScrollingEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !player.getMainHandItem().getItem().equals(ModItems.MODEL_HAMMER.get()) || AIBuilder.model == null) return;
        handleMouseScroll((float) event.getDeltaY());
        event.setCanceled(true);
    }

    public static void mouseButtonEvent(InputEvent.MouseButton event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !player.getMainHandItem().getItem().equals(ModItems.MODEL_HAMMER.get()) || AIBuilder.model == null) return;
        if (event.getButton() == InputConstants.MOUSE_BUTTON_RIGHT && event.getAction() == InputConstants.PRESS) handleRightClick(event);
    }

    public static void handleRightClick(InputEvent.MouseButton event) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        try (Level level = player.level()) {
            Vec3 eyePos = player.getEyePosition(1.0F);
            Vec3 lookVec = player.getLookAngle();
            Vec3 reachVec = eyePos.add(lookVec.scale(1000));

            BlockHitResult result = level.clip(new net.minecraft.world.level.ClipContext(
                eyePos,
                reachVec,
                net.minecraft.world.level.ClipContext.Block.OUTLINE,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                player
            ));
            ModelHammerItem.positionModel(result.getBlockPos(), result.getDirection());
            event.setCanceled(true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        PacketHandler.INSTANCE.send(new PlaceModelPacket(), Minecraft.getInstance().getConnection().getConnection());
    }

    public static void undoModel() {
        PacketHandler.INSTANCE.send(new UndoModelPacket(), Minecraft.getInstance().getConnection().getConnection());
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
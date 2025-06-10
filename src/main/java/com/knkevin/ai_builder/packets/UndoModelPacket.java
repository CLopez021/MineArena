package com.knkevin.ai_builder.packets;

import com.knkevin.ai_builder.AIBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

/**
 * A packet sent from the client to server in order to undo the last placement of the loaded Model.
 */
public class UndoModelPacket {
    public UndoModelPacket() {}

    public void encode(FriendlyByteBuf buffer) {}

    public static UndoModelPacket decode(FriendlyByteBuf buffer) {
        return new UndoModelPacket();
    }

    /**
     * If the player is not null and there is a loaded Model, then undo the last placement of the Model.
     */
    public void handle(CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player == null) return;
        ctx.enqueueWork(() -> {
            if (AIBuilder.model != null) {
                AIBuilder.model.undo(player.level());
                player.sendSystemMessage(Component.literal("Undo successful."));
            }
        });
        ctx.setPacketHandled(true);
    }
}
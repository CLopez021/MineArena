package com.clopez021.mine_arena.packets;

import com.clopez021.mine_arena.MineArena;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

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
            if (MineArena.model != null) {
                MineArena.model.undo(player.level());
                player.sendSystemMessage(Component.literal("Undo successful."));
            }
        });
        ctx.setPacketHandled(true);
    }
}
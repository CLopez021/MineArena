package com.clopez021.mine_arena.packets;

import com.clopez021.mine_arena.MineArena;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

/**
 * A packet sent from the client to server in order to place the loaded Model.
 */
public class PlaceModelPacket {
    public PlaceModelPacket() {}

    public void encode(FriendlyByteBuf buffer) {}

    public static PlaceModelPacket decode(FriendlyByteBuf buffer) {
        return new PlaceModelPacket();
    }


    /**
     * If the player is not null and there is a loaded Model, then place the Model.
     */
    public void handle(CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player == null) return;
        ctx.enqueueWork(() -> {
            if (MineArena.model != null) {
                MineArena.model.placeBlocks(player.level());
                player.sendSystemMessage(Component.literal("Successfully placed model."));
            }
        });
        ctx.setPacketHandled(true);
    }
}
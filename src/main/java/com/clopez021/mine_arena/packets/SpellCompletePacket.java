package com.clopez021.mine_arena.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;
import com.clopez021.mine_arena.client.WandScreens;

public class SpellCompletePacket {
    public SpellCompletePacket() {}

    public void encode(FriendlyByteBuf buf) {}

    public static SpellCompletePacket decode(FriendlyByteBuf buf) { return new SpellCompletePacket(); }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> WandScreens.onSpellCompleteClient()));
        ctx.setPacketHandled(true);
    }
}

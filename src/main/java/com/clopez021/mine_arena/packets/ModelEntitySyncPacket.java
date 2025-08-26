package com.clopez021.mine_arena.packets;

import com.clopez021.mine_arena.entity.ModelEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ModelEntitySyncPacket {
	private final int entityId;
	private final float microScale;
	private final float minX, minY, minZ;
	private final Map<BlockPos, BlockState> voxels;

	public ModelEntitySyncPacket(int entityId, float microScale, float minX, float minY, float minZ, Map<BlockPos, BlockState> voxels) {
		this.entityId = entityId;
		this.microScale = microScale;
		this.minX = minX;
		this.minY = minY;
		this.minZ = minZ;
		this.voxels = voxels;
	}

	public static void encode(ModelEntitySyncPacket pkt, FriendlyByteBuf buf) {
		buf.writeVarInt(pkt.entityId);
		buf.writeFloat(pkt.microScale);
		buf.writeFloat(pkt.minX);
		buf.writeFloat(pkt.minY);
		buf.writeFloat(pkt.minZ);
		buf.writeVarInt(pkt.voxels.size());
		for (Map.Entry<BlockPos, BlockState> e : pkt.voxels.entrySet()) {
			BlockPos pos = e.getKey();
			BlockState state = e.getValue();
			buf.writeVarInt(pos.getX());
			buf.writeVarInt(pos.getY());
			buf.writeVarInt(pos.getZ());
			ResourceLocation key = Block.REGISTRY.getKey(state.getBlock());
			buf.writeResourceLocation(key);
		}
	}

	public static ModelEntitySyncPacket decode(FriendlyByteBuf buf) {
		int id = buf.readVarInt();
		float micro = buf.readFloat();
		float minX = buf.readFloat();
		float minY = buf.readFloat();
		float minZ = buf.readFloat();
		int size = buf.readVarInt();
		Map<BlockPos, BlockState> map = new HashMap<>(size);
		for (int i = 0; i < size; i++) {
			int x = buf.readVarInt();
			int y = buf.readVarInt();
			int z = buf.readVarInt();
			ResourceLocation key = buf.readResourceLocation();
			Block block = Block.REGISTRY.get(key);
			BlockState state = block.defaultBlockState();
			map.put(new BlockPos(x, y, z), state);
		}
		return new ModelEntitySyncPacket(id, micro, minX, minY, minZ, map);
	}

	public static void handle(ModelEntitySyncPacket pkt, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			var level = Minecraft.getInstance().level;
			if (level == null) return;
			var ent = level.getEntity(pkt.entityId);
			if (ent instanceof ModelEntity me) {
				me.microScale = pkt.microScale;
				me.minCorner.set(pkt.minX, pkt.minY, pkt.minZ);
				me.setVoxels(pkt.voxels);
			}
		});
		ctx.get().setPacketHandled(true);
	}
} 
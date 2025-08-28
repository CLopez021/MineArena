package com.clopez021.mine_arena.spell;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.util.HashMap;
import java.util.Map;

/**
 * Data class for SpellEntity initialization with JSON conversion helpers
 */
public class SpellEntityInitData {
	public final Map<BlockPos, BlockState> blocks;
	public final float microScale;

	public SpellEntityInitData(Map<BlockPos, BlockState> blocks, float microScale) {
		this.blocks = blocks;
		this.microScale = microScale;
	}

	/**
	 * Convert to JSON using primitives (same pattern as NBT sync)
	 */
	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		
		// Blocks as array of {x, y, z, blockId}
		JsonArray blocksArray = new JsonArray();
		for (var entry : blocks.entrySet()) {
			JsonObject blockEntry = new JsonObject();
			blockEntry.addProperty("x", entry.getKey().getX());
			blockEntry.addProperty("y", entry.getKey().getY());
			blockEntry.addProperty("z", entry.getKey().getZ());
			blockEntry.addProperty("blockId", BuiltInRegistries.BLOCK.getId(entry.getValue().getBlock()));
			blocksArray.add(blockEntry);
		}
		json.add("blocks", blocksArray);
		
		json.addProperty("microScale", microScale);
		
		return json;
	}

	/**
	 * Convert from JSON primitives back to SpellEntityInitData (same pattern as rebuildBlocksFromData)
	 */
	public static SpellEntityInitData fromJson(JsonObject json) {
		// Rebuild blocks map
		Map<BlockPos, BlockState> blocks = new HashMap<>();
		JsonArray blocksArray = json.getAsJsonArray("blocks");
		for (int i = 0; i < blocksArray.size(); i++) {
			JsonObject blockEntry = blocksArray.get(i).getAsJsonObject();
			BlockPos pos = new BlockPos(
				blockEntry.get("x").getAsInt(),
				blockEntry.get("y").getAsInt(),
				blockEntry.get("z").getAsInt()
			);
			int blockId = blockEntry.get("blockId").getAsInt();
			Block block = BuiltInRegistries.BLOCK.byId(blockId);
			blocks.put(pos, block.defaultBlockState());
		}

		float microScale = json.get("microScale").getAsFloat();

		return new SpellEntityInitData(blocks, microScale);
	}
} 
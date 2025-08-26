package com.clopez021.mine_arena.models.util;

import com.clopez021.mine_arena.models.Model;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds a voxel map from a Model without placing blocks in the world.
 */
public class VoxelHelper {
	/**
	 * Uses the model's existing pipeline to compute a map of local block positions to block states.
	 * @param model The loaded model.
	 * @return A defensive copy of the model's texture-to-blocks mapping for use in entity rendering.
	 */
	public static Map<BlockPos, BlockState> buildVoxels(Model model) {
		if (model == null) return Map.of();
		Map<BlockPos, BlockState> blocks = model.getTextureToBlocks();
		return new HashMap<>(blocks);
	}
} 